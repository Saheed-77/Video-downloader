plugins {
    // AGP 9 compiles Kotlin itself ("built-in Kotlin"), so no separate kotlin-android plugin is needed.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.saheed.videodownloader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.saheed.videodownloader"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Decision 12: every target phone is 64-bit ARM, so ship only that (roughly halves the APK).
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            // youtubedl-android runs python/ffmpeg from the native lib folder, so the .so files
            // must be unpacked on install. This makes AGP set android:extractNativeLibs="true".
            useLegacyPackaging = true
        }
    }

    // In JVM unit tests Android calls like Log.w do nothing instead of throwing "not mocked".
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)
    // Coil: loads and caches the video thumbnails shown in the picker.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // Android's own org.json is an empty stub in JVM unit tests; this is the real one.
    testImplementation(libs.json)
}
