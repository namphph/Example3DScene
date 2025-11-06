#pragma once
#include <string>

bool ApplyTextureToGlb(
    const std::string &inputGlb,
    const std::string &texturePath,
    const std::string &targetMaterialName,
    const std::string &outputGlb);
