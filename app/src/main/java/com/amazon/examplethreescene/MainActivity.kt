package com.amazon.examplethreescene

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.amazon.examplethreescene.R
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.amazon.examplethreescene.customview.UvPaintMaskView
import com.amazon.examplethreescene.utils.AssimpHelper
import com.amazon.examplethreescene.utils.FileUtils
import io.github.sceneview.SceneView
import io.github.sceneview.collision.Vector3
import io.github.sceneview.material.setTexture
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {
    private lateinit var sceneView: SceneView
    private lateinit var loadingView: View
    var prevDistanceX: Float = 0.0F
    var prevDistanceY: Float = 0.0F
    lateinit var texture: Texture
    private var drawableBitmap: Bitmap?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        sceneView = findViewById(R.id.sceneView)
        loadingView = findViewById(R.id.loadingView)

        lifecycleScope.launch {
            val hdrFile = "environments/studio_small_09_2k.hdr"

            sceneView.environmentLoader.loadHDREnvironment(hdrFile).apply {
                sceneView.indirectLight = this?.indirectLight
                sceneView.skybox = this?.skybox
            }

            sceneView.cameraNode.apply {
                position = _root_ide_package_.io.github.sceneview.math.Position(z = 4.0f)
            }
            val outFile = File(filesDir, "converted.glb")

            val modelFile2 = "models/roblox_phat.glb"
            val modelInstance2 = sceneView.modelLoader.createModelInstance(modelFile2)

            var uvBitmap = BitmapFactory.decodeResource(resources, R.drawable.ooo)
            val mutableBitmap = uvBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)
            val paint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
            }

// Lấy chiều rộng & chiều cao của ảnh
            val centerY = mutableBitmap.height / 2f +100

// Vẽ đường ngang qua giữa ảnh (từ trái sang phải)
            canvas.drawLine(0f, centerY, mutableBitmap.width.toFloat(), centerY, paint)
            findViewById<ImageView>(R.id.imgBitmap).setImageBitmap(mutableBitmap)

            val engine = sceneView.engine
            val uv = BitmapFactory.decodeResource(resources, R.drawable.jjj)
            findViewById<UvPaintMaskView >(R.id.customPaintView).setUvBitmap(uv)
            findViewById<UvPaintMaskView >(R.id.customPaintView).onBitmapUpdated = {
                Log.d("112233","ABCDED")
                val drawMutableMap = findViewById<UvPaintMaskView >(R.id.customPaintView).getResultBitmap()
                drawableBitmap = drawMutableMap
//                findViewById<ImageView>(R.id.imgBitmap1).setImageBitmap(drawMutableMap)
                drawMutableMap?.let {
                    texture = createTextureFromBitmap(engine,it)

                    modelInstance2.materialInstances.forEach { mat ->
                        val sampler = TextureSampler(
                            TextureSampler.MinFilter.LINEAR,
                            TextureSampler.MagFilter.LINEAR,
                            TextureSampler.WrapMode.REPEAT
                        )
                        val parameters = mat.material.getParameters()
                        parameters.forEach { param ->
                            Log.d(
                                "SceneView",
                                "🧩 Param: ${param.name}, type: ${param.type}"
                            )
                        }
                        try {
                            mat.setParameter("baseColorIndex", 0)
                            mat.setTexture("baseColorMap",texture,sampler)
                            sceneView.invalidate()
                            Log.d("SceneView", "✅ Gán texture thành công cho ${mat.name}")
                        } catch (e: Exception) {
                            Log.e("SceneView", "❌ Không gán được texture: ${e.message}")
                        }
                    }
                }
            }

            modelInstance2.materialInstances.forEach { mat ->
                val parameters = mat.material.getParameters()
                parameters.forEach { param ->
                    Log.d(
                        "ccc",
                        "🧩 Param: ${param.name}, type: ${param.type}"
                    )
                }
            }

            val modelNode2 = ModelNode(
                modelInstance = modelInstance2,
                scaleToUnits = 2.0f,
            )

            modelNode2.scale = _root_ide_package_.io.github.sceneview.math.Scale(0.05f)
//            sceneView.addChildNode(modelNode)
//            sceneView.addChildNode(modelNode1)
            sceneView.addChildNode(modelNode2)

            // --- Thay đổi màu GLB --
//            modelInstance.materialInstances.forEach { materialInstance ->
//                materialInstance.setParameter("baseColorFactor", Colors.RgbType.LINEAR,
//                    0f, 1f, 0f)
//            }
            loadingView.isGone = true
        }
        findViewById<Button>(R.id.btnExport).setOnClickListener {
//            convertGlbToFbxWithUVMap()
            convertGlbToUVMap()
        }
//        convertGlbToFbx()
    }

    private fun convertGlbToFbxWithUVMap() {
        val inputPath = FileUtils.copyAssetToAppStorage(this, "models/ooo.glb", "ooo.glb")
        drawableBitmap?.let {
            val texturePath = FileUtils.saveBitmapToAppFiles(this,it,"imageone")
            val cacheDir = cacheDir.absolutePath + "/converted_model.fbx"
            inputPath?.let { inputPath ->
                texturePath?.let { texturePath ->
                    val success = exportFbxWithBitmap(this,inputPath, it,cacheDir)
                    if(success) {
                        Log.d("112233","Thanh cong")
                        saveToDownload(this@MainActivity,cacheDir,"pppp")
                    }else{
                        Log.d("112233", "That bai")
                    }
                }
            }
        }
    }

    private fun convertGlbToUVMap() {
        val inputPath = FileUtils.copyAssetToAppStorage(this, "models/a.glb", "a.glb")
        val width = 1024
        val height = 1024
        inputPath?.let {
            val uvPath = AssimpHelper().generateUVMap(inputPath,width,height)
            if (uvPath != null) {
                Log.d("AssimpJNI", "✅ UV map saved at $uvPath")
                uvPath?.let { image ->
                    findViewById<ImageView>(R.id.imgBitmap1).setImageBitmap(image as Bitmap?)
                }
            }
        }
    }

    fun exportFbxWithBitmap(context: Context, glbPath: String, bitmap: Bitmap, outputFbxPath: String): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        return AssimpHelper().bakeBitmapToFbx(glbPath, pixels, width, height, outputFbxPath)
    }

    private fun convertGlbToFbx() {
        val inputPath = FileUtils.copyAssetToAppStorage(this, "models/a.glb", "a.glb")
        val cacheDir = cacheDir.absolutePath
        inputPath?.let { ipath ->
            Log.d("112233", ipath);
            Log.d("112233", cacheDir);
            val success = AssimpHelper().convertGlbToFbx(inputPath, cacheDir)
            val file = File("${cacheDir}")

            if (file.exists()) {
                val sizeInBytes = file.length()
                val sizeInKb = sizeInBytes / 1024.0
                val sizeInMb = sizeInKb / 1024.0

                Log.d("Assimp", "✅ FBX file size: $sizeInBytes bytes (${String.format("%.2f", sizeInMb)} MB)")
            } else {
                Log.e("Assimp", "❌ File not found")
            }
            if (success) {
                Log.d("Assimp", "✅ Convert thành công")
                Log.d("Assimp", cacheDir)
//                saveToDownload(this@MainActivity,cacheDir+"/converted_model.fbx","bb")
            } else {
                Log.e("Assimp", "❌ Convert thất bại")
            }
        }
    }

    fun saveToDownload(context: Context, cacheFilePath: String, outputFileName: String): Boolean {
        val cacheFile = File(cacheFilePath)
        if (!cacheFile.exists()) {
            Log.e("Assimp", "❌ Cache file not found: $cacheFilePath")
            return false
        }

        // Đợi file ghi xong (nếu export vừa hoàn tất)
        var waitCount = 0
        while (cacheFile.length() == 0L && waitCount < 10) {
            Thread.sleep(200)
            waitCount++
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "model/fbx") // hoặc "application/octet-stream"
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: return false.also { Log.e("Assimp", "❌ Failed to create file in Downloads") }

        return try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                FileInputStream(cacheFile).use { input ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return false

            Log.d("Assimp", "✅ Copied to Downloads as $outputFileName (${cacheFile.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.d("Assimp",e.toString())
            e.printStackTrace()
            false
        } finally {
            cacheFile.delete() // Xóa file cache nếu muốn
        }
    }

    fun createTextureFromBitmap(engine: Engine, bitmap: Bitmap): Texture {
        val texture = Texture.Builder()
            .width(bitmap.width)
            .height(bitmap.height)
            .levels(1)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.SRGB8_A8)
            .build(engine)

        val rgbaBuffer = convertBitmapToRGBA(bitmap)
        val pixelBuffer = Texture.PixelBufferDescriptor(
            rgbaBuffer,
            Texture.Format.RGBA,
            Texture.Type.UBYTE
        )

        texture.setImage(engine, 0, pixelBuffer)
        return texture
    }

    fun convertBitmapToRGBA(bitmap: Bitmap): ByteBuffer {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val buffer = ByteBuffer.allocateDirect(bitmap.width * bitmap.height * 4)
        for (p in pixels) {
            // ARGB -> RGBA
            buffer.put(((p shr 16) and 0xFF).toByte()) // R
            buffer.put(((p shr 8) and 0xFF).toByte())  // G
            buffer.put((p and 0xFF).toByte())          // B
            buffer.put(((p shr 24) and 0xFF).toByte()) // A
        }
        buffer.rewind()
        return buffer
    }
}

data class Ray(val origin: Vector3, val direction: Vector3)

object TextureHelper {
    fun setBitmap(
        engine: Engine,
        texture: Texture,
        level: Int,
        buffer: ByteBuffer,
        width: Int,
        height: Int
    ) {
        val pixelBufferDescriptor = Texture.PixelBufferDescriptor(
            buffer,
            Texture.Format.RGBA,
            Texture.Type.UBYTE
        )
        texture.setImage(engine, level, pixelBufferDescriptor)
    }
}


