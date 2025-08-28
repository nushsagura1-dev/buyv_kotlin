plugins {
    kotlin("multiplatform")
    id("com.android.library")
    // id("com.google.gms.google-services") // only needed if you use Firebase on Android
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {

    applyDefaultHierarchyTemplate()

    // Android target
    androidTarget {
        // Android target configuration
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("io.insert-koin:koin-core:3.5.3")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.insert-koin:koin-android:3.5.3")
            }
        }
        val iosMain by getting {
            // No need to add coroutines here, it's included from commonMain
        }
    }
}

android {
    namespace = "com.project.e_commerce"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Add Android-only dependencies here if needed
    // implementation("com.google.firebase:firebase-crashlytics:18.3.2")
    // implementation("com.google.firebase:firebase-analytics:21.2.0")
}
