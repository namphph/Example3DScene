#include <jni.h>
#include <string>
#include <android/log.h>
#include <assimp/include/assimp/Importer.hpp>
#include <assimp/include/assimp/Exporter.hpp>
#include <assimp/include/assimp/scene.h>
#include <assimp/include/assimp/postprocess.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "AssimpJNI", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "AssimpJNI", __VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_amazon_examplethreescene_utils_AssimpHelper_convertGlbToFbx(
        JNIEnv* env,
        jobject thiz,
        jstring jInputPath,
        jstring jOutputPath) {

    if (jInputPath == nullptr || jOutputPath == nullptr) {
        LOGE("❌ Null jstring arguments received");
        return JNI_FALSE;
    }

    const char* inPath = env->GetStringUTFChars(jInputPath, nullptr);
    const char* outPath = env->GetStringUTFChars(jOutputPath, nullptr);

    if (!inPath || !outPath) {
        LOGE("❌ Failed to get UTF chars from Java strings");
        if (inPath) env->ReleaseStringUTFChars(jInputPath, inPath);
        if (outPath) env->ReleaseStringUTFChars(jOutputPath, outPath);
        return JNI_FALSE;
    }

    LOGI("🚀 Starting conversion GLB → FBX");
    LOGI("Input: %s", inPath);
    LOGI("Output: %s", outPath);

    Assimp::Importer importer;
    const aiScene* scene = importer.ReadFile(inPath,
                                             aiProcess_Triangulate |
                                             aiProcess_JoinIdenticalVertices |
                                             aiProcess_ImproveCacheLocality |
                                             aiProcess_PreTransformVertices);

    if (!scene) {
        LOGE("❌ Assimp import failed: %s", importer.GetErrorString());
        env->ReleaseStringUTFChars(jInputPath, inPath);
        env->ReleaseStringUTFChars(jOutputPath, outPath);
        return JNI_FALSE;
    }

    Assimp::Exporter exporter;
    aiReturn ret = exporter.Export(scene, "fbx", outPath);

    if (ret != aiReturn_SUCCESS) {
        LOGE("❌ Assimp export failed with code: %d", ret);
    } else {
        LOGI("✅ Export succeeded!");
    }

    env->ReleaseStringUTFChars(jInputPath, inPath);
    env->ReleaseStringUTFChars(jOutputPath, outPath);

    return (ret == aiReturn_SUCCESS) ? JNI_TRUE : JNI_FALSE;
}
