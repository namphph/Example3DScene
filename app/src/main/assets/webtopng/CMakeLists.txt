cmake_minimum_required(VERSION 3.10)
project(glb_texture_converter)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(${CMAKE_SOURCE_DIR}/src)

# === Android build ===
if (ANDROID)
    message(STATUS "📱 Building for Android (ABI: ${ANDROID_ABI})")

    # --- Cấu hình đường dẫn libwebp ---
    set(WEBP_ROOT ${CMAKE_SOURCE_DIR}/third_party/libwebp)
    set(WEBP_SRC_DIR ${WEBP_ROOT}/src)

    # --- Include đầy đủ ---
    include_directories(
        ${WEBP_ROOT}
        ${WEBP_SRC_DIR}
        ${WEBP_SRC_DIR}/webp
    )

    # --- Thu thập mã nguồn libwebp ---
    file(GLOB_RECURSE WEBP_SOURCES
        ${WEBP_SRC_DIR}/*.c
    )

    # --- Tạo thư viện libwebp static ---
    add_library(webp STATIC ${WEBP_SOURCES})

    target_compile_definitions(webp PRIVATE
        -DANDROID
        -DWEBP_USE_THREAD=0
        -DWEBP_ENABLE_SIMD=0
    )

    # --- Tạo thư viện glbtex ---
    add_library(glbtex SHARED
        src/GlbWebpToPngConverter.cpp
        src/GlbTexHelper.cpp 
        src/stb_image_write_impl.cpp
    )

    target_link_libraries(glbtex
        webp
        android
        log
    )

# === Desktop (macOS/Linux) ===
else()
    set(LIBWEBP_DIR "/opt/homebrew/opt/webp")

    find_path(WEBP_INCLUDE_DIR
        NAMES webp/decode.h
        PATHS "${LIBWEBP_DIR}/include"
    )
    find_library(WEBP_LIBRARY
        NAMES webp
        PATHS "${LIBWEBP_DIR}/lib"
    )

    if (NOT WEBP_INCLUDE_DIR OR NOT WEBP_LIBRARY)
        message(FATAL_ERROR "❌ Không tìm thấy libwebp tại ${LIBWEBP_DIR}")
    endif()

    message(STATUS "✅ Found libwebp include: ${WEBP_INCLUDE_DIR}")
    message(STATUS "✅ Found libwebp library: ${WEBP_LIBRARY}")

    include_directories(${WEBP_INCLUDE_DIR})

    add_library(glbtex SHARED
        src/GlbWebpToPngConverter.cpp
        src/GlbTexHelper.cpp
        src/stb_image_write_impl.cpp
    )
    target_link_libraries(glbtex ${WEBP_LIBRARY})
endif()
