package com.amazon.examplethreescene.utils

import android.util.Log

object AssimpHelper {
    init {
        System.loadLibrary("assimp")
    }

    external fun convertGlbToFbx(input: String, output: String): Boolean
}
