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
            // .msi needs the WiX toolset; without it, use createDistributable / packageZip below.
            targetFormats(TargetFormat.Msi)
            packageName = "VideoDownloader"
            packageVersion = "1.0.0"
            description = "Download videos and MP3s with yt-dlp"
            vendor = "Saheed"
            // The packaged app carries its own trimmed-down Java with only these parts
            // (from `gradlew :desktop:suggestRuntimeModules`, plus jdk.crypto.ec for HTTPS on Java 21).
            modules("java.instrument", "java.net.http", "jdk.unsupported", "jdk.crypto.ec")
            // The .exe's icon (Play-down); drawn by icons/make_icons.py. The window icon follows the theme.
            windows { iconFile.set(project.file("icons/VideoDownloader.ico")) }
        }
    }
}

// `gradlew :desktop:packageZip` → desktop/build/dist/VideoDownloader-<version>-windows.zip:
// the VideoDownloader folder (exe + its own Java), ready to attach to a GitHub Release.
tasks.register<Zip>("packageZip") {
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app"))
    archiveFileName.set("VideoDownloader-1.0.0-windows.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
}
