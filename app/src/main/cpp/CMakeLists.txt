cmake_minimum_required(VERSION 3.22.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
        assimp

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        AssimpHelper.cpp )
# *** ADD THIS LINE ***
# This tells CMake to add the 'assimp/include' directory to the include path for the 'assimp1' target.
# Adjust the path if your Assimp headers are in a different location.
# Include Assimp headers
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp)
target_include_directories(assimp PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/)

# Link against other libraries if needed, like the Android log library
find_library(
        log-lib
        log
)

target_link_libraries(
        assimp
        ${log-lib}
)
