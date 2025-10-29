plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.amazon.examplethreescene"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.amazon.examplethreescene"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Thêm flags cho C++ và ABI filters
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
        ndkVersion = "29.0.14206865"
    }

    // Cấu hình CMake chung cho toàn project
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        // Thư mục chứa các file .so nếu có copy sẵn
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.sceneview)
}
