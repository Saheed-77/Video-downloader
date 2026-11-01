// Logic shared by the Android app and the desktop app: plain Kotlin, no Android classes.
plugins {
    alias(libs.plugins.kotlin.jvm)
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
    // org.json is built into Android, so only compile against it here; the desktop app adds it
    // for real. (Bundling it into the Android app would clash with Android's own copy.)
    compileOnly(libs.json)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}
