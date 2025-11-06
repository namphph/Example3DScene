package com.amazon.examplethreescene

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.amazon.examplethreescene.customview.UvPaintMaskView
import com.amazon.examplethreescene.utils.AssimpHelper
import com.amazon.examplethreescene.utils.FileUtils
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import io.github.sceneview.SceneView
import io.github.sceneview.material.setTexture
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    private lateinit var sceneView: SceneView
    private lateinit var loadingView: View
    private var drawableBitmap: Bitmap? = null
    private lateinit var texture: Texture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        sceneView = findViewById(R.id.sceneView)
        loadingView = findViewById(R.id.loadingView)

        val inputPath = FileUtils.copyAssetToAppStorage(this, "models/rbrtophat.glb", "rbr.glb")
        val outputGlbFile = File(cacheDir, "rbr2.glb")
        val outputGlb = outputGlbFile.absolutePath
        val textureDir = File(cacheDir, "textures").apply { mkdirs() }.absolutePath

        lifecycleScope.launch(Dispatchers.IO) {
            val helper = AssimpHelper()
            val ok = try {
                if (inputPath.isNullOrEmpty()) {
                    Log.e("GLBTEX", "❌ Input GLB path is empty")
                    false
                } else {
                    Log.d("GLBTEX", "Start convert: input=$inputPath output=$outputGlb")
                    helper.convertWebpTexturesInGlb(inputPath, outputGlb, textureDir)
                }
            } catch (e: Throwable) {
                Log.e("GLBTEX", "Exception during convert: ${e.message}", e)
                false
            }

            Log.d("GLBTEX", "convertWebpTexturesInGlb result = $ok")

            val modelToLoadFile = if (ok && outputGlbFile.exists() && isValidGlb(outputGlbFile)) {
                Log.d("GLBTEX", "✅ Converted GLB validated, will load: ${outputGlbFile.absolutePath}")
                saveToDownloadSafe(this@MainActivity, outputGlbFile.absolutePath, "rbr2.glb")
                outputGlbFile
            } else {
                Log.w("GLBTEX", "⚠️ Converted GLB invalid or missing, falling back to original asset")
                null
            }

            withContext(Dispatchers.Main) {
                setupSceneAndLoadModel(modelToLoadFile ?: inputPath?.let { File(it) })
            }
        }

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            convertGlbToFbxWithUVMap()
        }
    }

    private fun isValidGlb(file: File): Boolean {
        try {
            if (!file.exists() || file.length() < 12) return false
            FileInputStream(file).use { fis ->
                val header = ByteArray(12)
                val read = fis.read(header)
                if (read != 12) return false
                if (header[0] != 0x67.toByte() || header[1] != 0x6C.toByte() ||
                    header[2] != 0x54.toByte() || header[3] != 0x46.toByte()
                ) return false
                val version = ((header[4].toInt() and 0xFF)
                        or ((header[5].toInt() and 0xFF) shl 8)
                        or ((header[6].toInt() and 0xFF) shl 16)
                        or ((header[7].toInt() and 0xFF) shl 24))
                return version == 2
            }
        } catch (e: Exception) {
            Log.e("GLBTEX", "isValidGlb error: ${e.message}", e)
            return false
        }
    }

    private fun setupSceneAndLoadModel(modelFile: File?) {
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            try {
                val hdrFile = "environments/studio_small_09_2k.hdr"

                // Load environment (synchronous)
                val environment = sceneView.environmentLoader.loadHDREnvironment(hdrFile)
                environment?.let {
                    sceneView.indirectLight = it.indirectLight
                    sceneView.skybox = it.skybox
                }

                // Camera
                sceneView.cameraNode.position = io.github.sceneview.math.Position(z = 4.0f)

                // Create model instance
                val modelInstance = try {
                    if (modelFile != null && modelFile.exists()) {
                        Log.d("GLBTEX", "📂 Loading model from file system: ${modelFile.absolutePath}")
                        // ✅ Dùng File thay vì string
                        sceneView.modelLoader.createModelInstance(modelFile)
                    } else {
                        Log.w("GLBTEX", "⚠️ Fallback to asset model: models/rbrtophat.glb")
                        sceneView.modelLoader.createModelInstance("models/rbrtophat.glb")
                    }
                } catch (e: Exception) {
                    Log.e("GLBTEX", "Exception while creating model instance: ${e.message}", e)
                    sceneView.modelLoader.createModelInstance("models/rbrtophat.glb")
                }

                // Add node
                val modelNode = ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = 2.0f
                )
                modelNode.scale = io.github.sceneview.math.Scale(5f)
                sceneView.addChildNode(modelNode)
                loadingView.isGone = true

                // Runtime paint texture hook
                val engine = sceneView.engine
                val uv = BitmapFactory.decodeResource(resources, R.drawable.jjj)
                val paintView = findViewById<UvPaintMaskView>(R.id.customPaintView)
                paintView.setUvBitmap(uv)
                paintView.onBitmapUpdated = {
                    val drawBitmap = paintView.getResultBitmap()
                    drawableBitmap = drawBitmap
                    findViewById<ImageView>(R.id.imgBitmap1).setImageBitmap(drawBitmap)
                    drawBitmap?.let { bmp ->
                        val texture = createTextureFromBitmap(engine, bmp)
                        modelInstance.materialInstances.forEach { mat ->
                            val sampler = TextureSampler(
                                TextureSampler.MinFilter.LINEAR,
                                TextureSampler.MagFilter.LINEAR,
                                TextureSampler.WrapMode.REPEAT
                            )
                            try {
                                mat.setParameter("baseColorIndex", 0)
                                mat.setTexture("baseColorMap", texture, sampler)
                                sceneView.invalidate()
                            } catch (e: Exception) {
                                Log.e("SceneView", "❌ setTexture failed: ${e.message}", e)
                            }
                        }
                    }
                }

            } catch (e: Throwable) {
                Log.e("GLBTEX", "Error after model load: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error during model setup", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun convertGlbToFbxWithUVMap() {
        val inputPath = FileUtils.copyAssetToAppStorage(this, "models/ooo.glb", "ooo.glb")
        drawableBitmap?.let {
            val texturePath = FileUtils.saveBitmapToAppFiles(this, it, "imageone")
            val cacheFile = File(cacheDir, "converted_model.fbx").absolutePath
            inputPath?.let { input ->
                texturePath?.let { texture ->
                    val success = exportFbxWithBitmap(this, input, it, cacheFile)
                    if (success) {
                        Log.d("112233", "✅ Xuất FBX thành công")
                        saveToDownload(this, cacheFile, "pppp")
                    } else {
                        Log.d("112233", "❌ Xuất FBX thất bại")
                    }
                }
            }
        }
    }

    private fun exportFbxWithBitmap(context: Context, glbPath: String, bitmap: Bitmap, outputFbxPath: String): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return AssimpHelper().bakeBitmapToFbx(glbPath, pixels, width, height, outputFbxPath)
    }

    private fun saveToDownload(context: Context, cacheFilePath: String, outputFileName: String): Boolean {
        return saveToDownloadSafe(context, cacheFilePath, outputFileName)
    }

    private fun saveToDownloadSafe(context: Context, cacheFilePath: String, outputFileName: String): Boolean {
        val cacheFile = File(cacheFilePath)
        if (!cacheFile.exists()) {
            Log.e("Assimp", "❌ Cache file not found: $cacheFilePath")
            return false
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "model/gltf-binary")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return false
        return try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                FileInputStream(cacheFile).use { input ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            Log.d("Assimp", "✅ Copied to Downloads as $outputFileName")
            true
        } catch (e: Exception) {
            Log.e("Assimp", "Copy failed: ${e.message}", e)
            false
        }
    }

    private fun createTextureFromBitmap(engine: Engine, bitmap: Bitmap): Texture {
        val texture = Texture.Builder()
            .width(bitmap.width)
            .height(bitmap.height)
            .levels(1)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.SRGB8_A8)
            .build(engine)

        val rgbaBuffer = convertBitmapToRGBA(bitmap)
        val pixelBuffer = Texture.PixelBufferDescriptor(
            rgbaBuffer, Texture.Format.RGBA, Texture.Type.UBYTE
        )
        texture.setImage(engine, 0, pixelBuffer)
        return texture
    }

    private fun convertBitmapToRGBA(bitmap: Bitmap): ByteBuffer {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val buffer = ByteBuffer.allocateDirect(bitmap.width * bitmap.height * 4)
        for (p in pixels) {
            buffer.put(((p shr 16) and 0xFF).toByte())
            buffer.put(((p shr 8) and 0xFF).toByte())
            buffer.put((p and 0xFF).toByte())
            buffer.put(((p shr 24) and 0xFF).toByte())
        }
        buffer.rewind()
        return buffer
    }
}
