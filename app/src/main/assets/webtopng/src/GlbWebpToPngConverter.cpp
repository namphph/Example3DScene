#define TINYGLTF_IMPLEMENTATION
#define TINYGLTF_NO_STB_IMAGE
#define TINYGLTF_NO_EXTERNAL_IMAGE
#define TINYGLTF_NOEXCEPTION

#include "tiny_gltf.h"
#include <webp/decode.h>
#include <vector>
#include <string>
#include <fstream>
#include <iostream>

#include "GlbWebpToPngConverter.h"

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "GLBTEX_NATIVE"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...) printf("[DEBUG] " __VA_ARGS__), printf("\n")
#define LOGE(...) fprintf(stderr, "[ERROR] " __VA_ARGS__), fprintf(stderr, "\n")
#endif

static inline size_t align4(size_t v) {
    return (v + 3) & ~static_cast<size_t>(3);
}

bool ConvertWebpTexturesInGlb(
    const std::string &inputGlb,
    const std::string &outputGlb,
    const std::string &outputTextureDir) {

    LOGD("🚀 ConvertWebpTexturesInGlb() started");
    LOGD("Input: %s", inputGlb.c_str());
    LOGD("Output: %s", outputGlb.c_str());

    tinygltf::TinyGLTF loader;
    tinygltf::TinyGLTF writer;
    tinygltf::Model model;
    std::string err, warn;

    loader.SetImageLoader([](tinygltf::Image *image, const int index, std::string *err, std::string *warn,
                             int req_width, int req_height, const unsigned char *bytes, int size,
                             void *user_data) -> bool {
        if (!bytes || size <= 0) {
            if (err) *err = "Empty image buffer";
            return false;
        }
        image->image.assign(bytes, bytes + size);
        image->width = req_width;
        image->height = req_height;
        image->component = 0;
        image->bits = 8;
        image->pixel_type = TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE;
        return true;
    }, nullptr);

    bool ret = loader.LoadBinaryFromFile(&model, &err, &warn, inputGlb);
    if (!warn.empty()) LOGD("⚠️ GLTF Warning: %s", warn.c_str());
    if (!err.empty()) LOGE("❌ GLTF Error: %s", err.c_str());
    if (!ret) {
        LOGE("❌ Failed to load GLB file");
        return false;
    }

    LOGD("✅ Loaded GLB successfully (images=%zu)", model.images.size());

    int webpCount = 0;

    for (size_t i = 0; i < model.images.size(); ++i) {
        auto &img = model.images[i];

        if (img.mimeType != "image/webp" && !tinygltf::IsDataURI(img.uri)) {
            continue;
        }

        webpCount++;
        int w = 0, h = 0;
        uint8_t *rgba = WebPDecodeRGBA(img.image.data(), static_cast<int>(img.image.size()), &w, &h);
        if (!rgba) {
            LOGE("❌ Failed to decode WebP image[%zu]", i);
            continue;
        }

        std::string pngName = "texture_" + std::to_string(i) + ".png";
        std::string pngPath = outputTextureDir + "/" + pngName;
        if (!stbi_write_png(pngPath.c_str(), w, h, 4, rgba, w * 4)) {
            LOGE("❌ Failed to save PNG: %s", pngPath.c_str());
            WebPFree(rgba);
            continue;
        }

        // Reload PNG bytes for embedding
        std::ifstream pngFile(pngPath, std::ios::binary);
        std::vector<unsigned char> pngData((std::istreambuf_iterator<char>(pngFile)),
                                           std::istreambuf_iterator<char>());
        pngFile.close();

        img.image = pngData;
        img.uri.clear();
        img.mimeType = "image/png";
        img.width = w;
        img.height = h;
        img.component = 4;
        img.bits = 8;
        img.pixel_type = TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE;

        WebPFree(rgba);
        LOGD("✅ Converted image[%zu] to PNG (%dx%d, %zu bytes)", i, w, h, img.image.size());
    }

    if (webpCount == 0) {
        LOGE("⚠️ No WEBP textures found — skipping conversion.");
        return false;
    }

    // Embed PNGs into buffer
    if (model.buffers.empty()) {
        model.buffers.emplace_back();
    }

    for (auto &img : model.images) {
        if (img.image.empty()) continue;

        size_t offset = align4(model.buffers[0].data.size());
        model.buffers[0].data.insert(model.buffers[0].data.end(), img.image.begin(), img.image.end());

        tinygltf::BufferView view;
        view.buffer = 0;
        view.byteOffset = static_cast<int>(offset);
        view.byteLength = static_cast<int>(img.image.size());
        model.bufferViews.push_back(view);

        img.bufferView = static_cast<int>(model.bufferViews.size() - 1);
        img.uri.clear();
    }

    // Fix EXT_texture_webp → source mapping
    for (size_t t = 0; t < model.textures.size(); ++t) {
        auto &tex = model.textures[t];
        auto it = tex.extensions.find("EXT_texture_webp");
        if (it != tex.extensions.end()) {
            int sourceIndex = -1;
            const tinygltf::Value &ext = it->second;
            if (ext.Has("source")) {
                const tinygltf::Value &srcVal = ext.Get("source");
                if (srcVal.IsInt()) sourceIndex = srcVal.Get<int>();
            }
            if (sourceIndex >= 0 && sourceIndex < (int)model.images.size()) {
                tex.source = sourceIndex;
                LOGD("🔧 Fixed texture[%zu] → image[%d]", t, sourceIndex);
            }
            tex.extensions.erase(it);
        }
    }

    LOGD("💾 Writing updated GLB to %s ...", outputGlb.c_str());
    bool writeOK = writer.WriteGltfSceneToFile(&model, outputGlb, true, true, true, true);
    if (!writeOK) {
        LOGE("❌ Failed to write GLB output");
        return false;
    }

    LOGD("✅ Finished converting %d WEBP textures to PNG and re-embedding", webpCount);
    return true;
}
