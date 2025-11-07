package com.amazon.examplethreescene.utils

import android.graphics.Bitmap

class AssimpHelper {
    companion object {
        init {
            System.loadLibrary("assimp")
        }
    }

    external fun convertGlbToFbx(inputPath: String, cacheDir: String): Boolean

    external fun convertGlbAndTextureToFbx(
        glbPath: String,
        texturePath: String,
        outputFbxPath: String
    ): Boolean

    external fun exportGlbWithBitmapTextureToFbx(
        glbPath: String,
        texturePngPath: String,
        outputFbxPath: String
    ): Boolean

    external fun bakeBitmapToFbx(
        glbPath: String,
        bitmapPixels: IntArray,
        width: Int,
        height: Int,
        outputFbxPath: String
    ): Boolean

    external fun exportFbxWithTexture(glbPath: String, bitmapPath: String, outputFbxPath: String): Boolean

    external fun generateUVMap(glbPath: String, width: Int, height: Int): Bitmap?
}
