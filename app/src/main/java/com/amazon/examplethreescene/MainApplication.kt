package com.amazon.examplethreescene

import android.app.Application
import com.amazon.examplethreescene.utils.AssimpHelper

class MainApplication : Application() {
    var assimp: AssimpHelper?=null
    override fun onCreate() {
        super.onCreate()
        assimp = AssimpHelper()
    }
}
