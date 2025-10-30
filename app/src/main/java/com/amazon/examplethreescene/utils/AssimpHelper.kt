package com.amazon.examplethreescene.utils


class AssimpHelper {
    companion object {
        init {
            System.loadLibrary("assimp")
        }
    }

    external fun convertGlbToFbx(inputPath: String, cacheDir: String): Boolean
}
