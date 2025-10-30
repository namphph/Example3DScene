package com.amazon.examplethreescene.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun copyAssetToAppStorage(context: Context, assetName: String, destFileName: String): String? {
        // Lưu trong thư mục riêng của app
        val appDir = context.getExternalFilesDir(null) ?: context.filesDir
        if (!appDir.exists()) appDir.mkdirs()

        val outFile = File(appDir, destFileName)
        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return outFile.absolutePath
    }

    fun copyDrawableToAppStorage(context: Context, drawableResId: Int, destFileName: String): String? {
        val appDir = context.getExternalFilesDir(null) ?: context.filesDir
        if (!appDir.exists()) appDir.mkdirs()

        val outFile = File(appDir, destFileName)
        try {
            val drawable = context.resources.getDrawable(drawableResId, context.theme)

            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }

            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return outFile.absolutePath
    }

    fun saveBitmapToAppFiles(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val file = File(context.filesDir, "$fileName.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
