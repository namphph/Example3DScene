package com.amazon.examplethreescene.utils

import android.content.Context
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
}
