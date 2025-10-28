package com.amazon.example3dscene

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.amazon.example3dscene.customview.UvPaintMaskView
import io.github.sceneview.SceneView
import io.github.sceneview.collision.Vector3
import io.github.sceneview.material.setTexture
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    private lateinit var sceneView: SceneView
    private lateinit var loadingView: View
    var prevDistanceX: Float = 0.0F
    var prevDistanceY: Float = 0.0F
    lateinit var texture: Texture
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

//            val modelFile = "models/grogu.glb"
//            val modelInstance = sceneView.modelLoader.createModelInstance(modelFile)
//
//            val modelNode = ModelNode(
//                modelInstance = modelInstance,
//                scaleToUnits = 2.0f,
//            )
//
//            modelNode.scale = _root_ide_package_.io.github.sceneview.math.Scale(1f)
//
//            val modelFile1 = "models/azaz.glb"
//            val modelInstance1 = sceneView.modelLoader.createModelInstance(modelFile1)
//
//            val modelNode1 = ModelNode(
//                modelInstance = modelInstance1,
//                scaleToUnits = 2.0f,
//            )
//
//            modelNode1.scale = _root_ide_package_.io.github.sceneview.math.Scale(0.05f)

            val modelFile2 = "models/qaz.glb"
            val modelInstance2 = sceneView.modelLoader.createModelInstance(modelFile2)

            var uvBitmap = BitmapFactory.decodeResource(resources, R.drawable.a)
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
            val uv = BitmapFactory.decodeResource(resources, R.drawable.a)
            findViewById<UvPaintMaskView >(R.id.customPaintView).setUvBitmap(uv)
            findViewById<UvPaintMaskView >(R.id.customPaintView).onBitmapUpdated = {
                Log.d("112233","ABCDED")
                val drawMutableMap = findViewById<UvPaintMaskView >(R.id.customPaintView).getResultBitmap()
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


