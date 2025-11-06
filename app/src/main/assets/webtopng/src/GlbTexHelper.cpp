#include <jni.h>
#include <string>
#include "GlbWebpToPngConverter.h"
#include "GlbTextureApplier.h"

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_minecraft_pe_addons_mods_utils_androidassimpt_AssimpHelper_convertWebpTexturesInGlb(
        JNIEnv *env,
        jobject /* this */,
        jstring inputGlb_,
        jstring outputGlb_,
        jstring outputTextureDir_) {

    const char *inputGlb = env->GetStringUTFChars(inputGlb_, nullptr);
    const char *outputGlb = env->GetStringUTFChars(outputGlb_, nullptr);
    const char *outputTextureDir = env->GetStringUTFChars(outputTextureDir_, nullptr);

    bool result = ConvertWebpTexturesInGlb(inputGlb, outputGlb, outputTextureDir);

    env->ReleaseStringUTFChars(inputGlb_, inputGlb);
    env->ReleaseStringUTFChars(outputGlb_, outputGlb);
    env->ReleaseStringUTFChars(outputTextureDir_, outputTextureDir);

    return result ? JNI_TRUE : JNI_FALSE;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_minecraft_pe_addons_mods_utils_androidassimpt_AssimpHelper_applyTextureToGlb(
        JNIEnv *env,
        jobject /* this */,
        jstring inputGlb_,
        jstring texturePath_,
        jstring targetMaterialName_,
        jstring outputGlb_) {

    const char *inputGlb = env->GetStringUTFChars(inputGlb_, nullptr);
    const char *texturePath = env->GetStringUTFChars(texturePath_, nullptr);
    const char *targetMaterialName = env->GetStringUTFChars(targetMaterialName_, nullptr);
    const char *outputGlb = env->GetStringUTFChars(outputGlb_, nullptr);

    bool result = ApplyTextureToGlb(inputGlb, texturePath, targetMaterialName, outputGlb);

    env->ReleaseStringUTFChars(inputGlb_, inputGlb);
    env->ReleaseStringUTFChars(texturePath_, texturePath);
    env->ReleaseStringUTFChars(targetMaterialName_, targetMaterialName);
    env->ReleaseStringUTFChars(outputGlb_, outputGlb);

    return result ? JNI_TRUE : JNI_FALSE;
}
