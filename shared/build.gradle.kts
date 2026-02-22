plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization") version "2.1.0"
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
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                
                // Dependency Injection
                implementation("io.insert-koin:koin-core:3.5.3")
                
                // Ktor Client (Networking)
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                implementation("io.ktor:ktor-client-logging:2.3.7")
                implementation("io.ktor:ktor-client-auth:2.3.7")
                
                // Firebase KMP (pour notifications uniquement maintenant)
                implementation("dev.gitlive:firebase-auth:2.1.0")
                implementation("dev.gitlive:firebase-firestore:2.1.0")
                implementation("dev.gitlive:firebase-storage:2.1.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.insert-koin:koin-android:3.5.3")
                // Ktor Client Android Engine
                implementation("io.ktor:ktor-client-okhttp:2.3.7")
                // Encrypted Storage
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
            }
        }
        val iosMain by getting {
            dependencies {
                // Ktor Client iOS Engine
                implementation("io.ktor:ktor-client-darwin:2.3.7")
            }
        }

        // Sprint 21: Test dependencies
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
                implementation("io.ktor:ktor-client-mock:2.3.7")
            }
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
