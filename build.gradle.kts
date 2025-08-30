buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.12.2")
    }
}

plugins {
    kotlin("android") version "2.2.10" apply false
    kotlin("multiplatform") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false // ✅ NEW
    id("com.android.application") version "8.12.2" apply false
    id("com.android.library") version "8.12.2" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
}
