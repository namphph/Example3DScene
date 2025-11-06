package com.amazon.examplethreescene.utils


class AssimpHelper {
    companion object {
        init {
            System.loadLibrary("assimp")
            System.loadLibrary("glbtex")
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

    external fun convertWebpTexturesInGlb(
        inputGlb: String,
        outputGlb: String,
        outputTextureDir: String
    ): Boolean
}
