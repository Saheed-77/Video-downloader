// The Windows desktop app: Compose for Desktop (JetBrains' version of Jetpack Compose for PCs),
// running yt-dlp.exe and ffmpeg.exe, and sharing its logic with the Android app via :shared.
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    // The window, buttons and lists, for this PC's operating system.
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3.desktop)
    // Lets coroutines update the window (the "Main" thread on desktop is Swing's).
    implementation(libs.kotlinx.coroutines.swing)
    // Android has org.json built in; the desktop needs the real library.
    implementation(libs.json)
    // Thumbnails, same library as on Android.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.saheed.videodownloader.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "VideoDownloader"
            packageVersion = "1.0.0"
        }
    }
}
