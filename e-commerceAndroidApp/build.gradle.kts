import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose") // ✅ NEW
    id("com.google.gms.google-services")
}

// Load local.properties for secrets
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.project.e_commerce.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.project.e_commerce.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Stripe publishable key from local.properties
        val stripeKey = localProperties.getProperty("stripe.publishable.key", "")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"$stripeKey\"")

        // Cloudinary credentials from local.properties (NEVER hardcode secrets in source)
        val cloudinaryApiKey = localProperties.getProperty("cloudinary.api.key", "")
        val cloudinaryApiSecret = localProperties.getProperty("cloudinary.api.secret", "")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"$cloudinaryApiKey\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"$cloudinaryApiSecret\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // ❌ REMOVE composeOptions (not needed with Kotlin 2.0+)

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(localProperties.getProperty("keystore.path", "e-commerceAndroidApp/buyv-release.keystore"))
            storePassword = localProperties.getProperty("keystore.password", "")
            keyAlias = localProperties.getProperty("keystore.alias", "buyv")
            keyPassword = localProperties.getProperty("keystore.key.password", "")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
            // Sign with release key so the APK can be installed on any device (no developer options needed)
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}


dependencies {
    implementation(project(":shared"))

    // Compose
    implementation("androidx.compose.ui:ui:1.9.0")
    implementation("androidx.compose.ui:ui-tooling:1.9.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.0")
    implementation("androidx.compose.foundation:foundation:1.9.0")
    implementation("androidx.compose.material:material:1.9.0")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.0")

    // Lifecycle
    val lifecycleVersion = "2.9.2"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2") // Updated for Compose/Koin compatibility
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    // Navigation
    val navigationVersion = "2.9.3"
    implementation("androidx.navigation:navigation-compose:$navigationVersion")
    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVersion")

    // Paging3
    val pagingVersion = "3.3.6"
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")

    // Koin
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    // Ktor (shared module uses implementation(), not api(), so redeclare here)
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-client-logging:2.3.7")
    implementation("io.ktor:ktor-client-auth:2.3.7")

    // OkHttp (used by Coil3) + Coroutines
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Firebase
    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    implementation("com.google.firebase:firebase-analytics:23.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.2")
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    // Facebook Login SDK (AUTH-002)
    implementation("com.facebook.android:facebook-login:18.0.0")
    // implementation("com.google.firebase:firebase-storage-ktx:21.0.2") // Commented out - using Cloudinary instead

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:3.1.1")
    implementation("com.cloudinary:cloudinary-core:2.3.2")

    // Stripe Payment SDK
    implementation("com.stripe:stripe-android:21.4.0")

    // UI / Animations / Media
    implementation("com.airbnb.android:lottie-compose:6.6.7")
    // ORIGINAL GLIDE DEPENDENCY (COMMENTED OUT):
    // implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("com.tbuonomo:dotsindicator:5.1.0")
    implementation("com.google.accompanist:accompanist-pager:0.36.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.36.0")

    // Media3
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // 2.18 — CameraX (video capture + preview)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    // 2.18 — GPUImage real-time GPU filters
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // EmojiCompat - consistent emoji rendering across Android versions
    implementation("androidx.emoji2:emoji2:1.5.0")
    implementation("androidx.emoji2:emoji2-bundled:1.5.0")

    // Core
    implementation("androidx.core:core-ktx:1.17.0")
    
    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Sprint 21: Test Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Compose UI Tests (Instrumented)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("io.insert-koin:koin-test:3.5.3")
    androidTestImplementation("io.insert-koin:koin-test-junit4:3.5.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.0")
}
