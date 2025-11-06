#include "GlbTextureApplier.h"
#include "tiny_gltf.h"
#include "stb_image.h"
#include "stb_image_write.h"
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "GlbTextureApplier"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::string ToLower(const std::string &s) {
    std::string r = s;
    std::transform(r.begin(), r.end(), r.begin(), ::tolower);
    return r;
}

// Convert WEBP → PNG tạm
static std::string ConvertWebpToPngTemp(const std::string &webpPath) {
    int w, h, ch;
    unsigned char *data = stbi_load(webpPath.c_str(), &w, &h, &ch, 4);
    if (!data) {
        std::cerr << "❌ Failed to load WEBP: " << webpPath << std::endl;
        return "";
    }

    std::string pngPath = webpPath + ".converted.png";
    if (!stbi_write_png(pngPath.c_str(), w, h, 4, data, w * 4)) {
        std::cerr << "❌ Failed to write PNG: " << pngPath << std::endl;
        stbi_image_free(data);
        return "";
    }

    stbi_image_free(data);
    return pngPath;
}

bool ApplyTextureToGlb(
    const std::string &inputGlb,
    const std::string &texturePath,
    const std::string &targetMaterialName,
    const std::string &outputGlb)
{
    LOGI("=== ApplyTextureToGlb START ===");
    LOGI("Input GLB: %s", inputGlb.c_str());
    LOGI("Texture Path: %s", texturePath.c_str());
    LOGI("Target Material: %s", targetMaterialName.c_str());
    LOGI("Output GLB: %s", outputGlb.c_str());

    tinygltf::TinyGLTF loader;
    tinygltf::Model model;
    std::string warn, err;

    // Custom image loader (avoid auto decoding)
    loader.SetImageLoader(
        [](tinygltf::Image *image, const int index, std::string *err, std::string *warn,
           int req_width, int req_height, const unsigned char *bytes, int size, void *user_data) -> bool {

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
        },
        nullptr);

    bool ret = loader.LoadBinaryFromFile(&model, &err, &warn, inputGlb);
    if (!warn.empty()) LOGE("Warn: %s", warn.c_str());
    if (!err.empty()) LOGE("Err: %s", err.c_str());
    if (!ret) {
        LOGE("❌ Failed to load GLB: %s", inputGlb.c_str());
        return false;
    }

    std::string texPath = texturePath;
    if (ToLower(texturePath).rfind(".webp") != std::string::npos) {
        std::string converted = ConvertWebpToPngTemp(texturePath);
        if (converted.empty()) {
            LOGE("❌ Failed to convert WEBP to PNG");
            return false;
        }
        texPath = converted;
    }

    std::ifstream texFile(texPath, std::ios::binary);
    if (!texFile.is_open()) {
        LOGE("❌ Cannot open texture file: %s", texPath.c_str());
        return false;
    }

    std::vector<unsigned char> texData((std::istreambuf_iterator<char>(texFile)),
                                       std::istreambuf_iterator<char>());
    texFile.close();

    LOGI("✅ Loaded texture bytes: %zu", texData.size());

    if (model.buffers.empty()) {
        model.buffers.emplace_back();
    }

    int img_w = 0, img_h = 0, img_ch = 0;
    if (!texData.empty()) {
        if (!stbi_info_from_memory(texData.data(), static_cast<int>(texData.size()), &img_w, &img_h, &img_ch)) {
            LOGE("⚠️ stbi_info_from_memory failed to read image header");
        }
    }

    auto align4 = [](size_t v)->size_t { return (v + 3) & ~static_cast<size_t>(3); };
    size_t offset = align4(model.buffers[0].data.size());
    size_t pad = offset - model.buffers[0].data.size();
    if (pad > 0) model.buffers[0].data.insert(model.buffers[0].data.end(), pad, 0);

    size_t byteLength = texData.size();
    model.buffers[0].data.insert(model.buffers[0].data.end(), texData.begin(), texData.end());

    tinygltf::BufferView view;
    view.buffer = 0;
    view.byteOffset = static_cast<int>(offset);
    view.byteLength = static_cast<int>(byteLength);
    view.target = 0;
    int bufferViewIndex = static_cast<int>(model.bufferViews.size());
    model.bufferViews.push_back(view);

    tinygltf::Image newImage;
    newImage.uri.clear();
    newImage.bufferView = bufferViewIndex;

    std::string lower = ToLower(texPath);
    if (lower.rfind(".png") != std::string::npos) newImage.mimeType = "image/png";
    else if (lower.rfind(".jpg") != std::string::npos || lower.rfind(".jpeg") != std::string::npos) newImage.mimeType = "image/jpeg";
    else if (lower.rfind(".webp") != std::string::npos) newImage.mimeType = "image/webp";
    else newImage.mimeType = "image/png";

    if (img_w > 0 && img_h > 0) {
        newImage.width = img_w;
        newImage.height = img_h;
    }
    newImage.component = (img_ch > 0) ? img_ch : 4;
    newImage.bits = 8;
    newImage.pixel_type = TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE;

    int newImageIndex = static_cast<int>(model.images.size());
    model.images.push_back(newImage);

    tinygltf::Texture newTexture;
    newTexture.source = newImageIndex;
    int newTexIndex = static_cast<int>(model.textures.size());
    model.textures.push_back(newTexture);

    LOGI("✅ Added texture index %d to model (image idx %d, bufferView %d, size %zu)",
         newTexIndex, newImageIndex, bufferViewIndex, byteLength);

    // === Apply to material (preserve all other material properties) ===
    std::string targetLower = ToLower(targetMaterialName);
    bool applied = false;
    for (size_t i = 0; i < model.materials.size(); ++i) {
        std::string matName = ToLower(model.materials[i].name);
        LOGI("Checking material[%zu]: %s", i, model.materials[i].name.c_str());
        if (matName.find(targetLower) != std::string::npos) {
            // Backup full material
            tinygltf::Material backup = model.materials[i];

            // Only change texture index
            backup.pbrMetallicRoughness.baseColorTexture.index = newTexIndex;

            // Ensure PBR + emission/normal data preserved
            backup.pbrMetallicRoughness.baseColorFactor = model.materials[i].pbrMetallicRoughness.baseColorFactor;
            backup.pbrMetallicRoughness.metallicFactor = model.materials[i].pbrMetallicRoughness.metallicFactor;
            backup.pbrMetallicRoughness.roughnessFactor = model.materials[i].pbrMetallicRoughness.roughnessFactor;
            backup.normalTexture = model.materials[i].normalTexture;
            backup.occlusionTexture = model.materials[i].occlusionTexture;
            backup.emissiveTexture = model.materials[i].emissiveTexture;
            backup.emissiveFactor = model.materials[i].emissiveFactor;
            backup.alphaMode = model.materials[i].alphaMode;
            backup.alphaCutoff = model.materials[i].alphaCutoff;
            backup.doubleSided = true;
            backup.name = model.materials[i].name;
            backup.extras = model.materials[i].extras;
            backup.extensions = model.materials[i].extensions;

            // Replace
            model.materials[i] = backup;
            applied = true;

            LOGI("✅ Applied new texture to material: %s", model.materials[i].name.c_str());
        }
    }

    if (!applied) {
        LOGE("⚠️ No material matched name: %s", targetMaterialName.c_str());
        return false;
    }

    LOGI("💾 Writing updated GLB to %s ...", outputGlb.c_str());

    {
        std::ofstream test(outputGlb, std::ios::binary);
        if (!test.is_open()) {
            LOGE("⚠️ Cannot open file for writing: %s", outputGlb.c_str());
        } else {
            LOGI("✅ Output path is writable.");
        }
    }

    tinygltf::TinyGLTF writer;
    bool saveOk = writer.WriteGltfSceneToFile(
        &model,
        outputGlb,
        true,  // embedImages
        true,  // embedBuffers
        true,  // prettyPrint
        true   // binary (.glb)
    );

    if (saveOk) {
        LOGI("✅ Wrote GLB successfully: %s", outputGlb.c_str());
        LOGI("🎉 Texture applied and GLB saved successfully!");
        return true;
    } else {
        LOGE("❌ Failed to write GLB file: %s", outputGlb.c_str());
        return false;
    }
}
