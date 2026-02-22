# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================
# 🔒 SECURITY - Remove all debug logs in release
# ============================================

# Remove all Log.v() calls
-assumenosideeffects class android.util.Log {
    public static *** v(...);
}

# Remove all Log.d() calls
-assumenosideeffects class android.util.Log {
    public static *** d(...);
}

# Remove all Log.i() calls
-assumenosideeffects class android.util.Log {
    public static *** i(...);
}

# Keep Log.w() and Log.e() for important warnings and errors
# (commented out - uncomment if you want to remove these too)
# -assumenosideeffects class android.util.Log {
#     public static *** w(...);
#     public static *** e(...);
# }

# Remove println debugging
-assumenosideeffects class kotlin.io.ConsoleKt {
    public static *** println(...);
}

# Remove custom DebugLog class logs (if exists)
-assumenosideeffects class com.project.e_commerce.android.utils.DebugLog {
    public static *** d(...);
    public static *** i(...);
    public static *** v(...);
}

# ============================================
# 📱 FIREBASE
# ============================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# ============================================
# 🌐 RETROFIT & OKHTTP
# ============================================
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ============================================
# 🎨 COMPOSE
# ============================================
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# ============================================
# 📦 KOTLIN SERIALIZATION
# ============================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# ============================================
# 🎥 EXOPLAYER / MEDIA3
# ============================================
-keep class androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ============================================
# 🖼️ COIL
# ============================================
-keep class coil3.** { *; }
-keep interface coil3.** { *; }
-dontwarn coil3.**

# ============================================
# ☁️ CLOUDINARY
# ============================================
-keep class com.cloudinary.** { *; }
-keepclassmembers class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# ============================================
# 🔐 KOIN DI
# ============================================
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keep class org.koin.dsl.** { *; }

# ============================================
# 📱 PROJECT DATA CLASSES
# ============================================
# Keep all data classes from your project
-keep class com.project.e_commerce.domain.model.** { *; }
-keep class com.project.e_commerce.data.remote.dto.** { *; }
-keep class com.project.e_commerce.android.domain.model.** { *; }

# Keep all repository implementations
-keep class com.project.e_commerce.data.repository.** { *; }
-keep class com.project.e_commerce.android.data.repository.** { *; }

# Keep ViewModels
-keep class com.project.e_commerce.android.presentation.viewModel.** { *; }

# Keep UseCases
-keep class com.project.e_commerce.android.domain.usecase.** { *; }

# ============================================
# 🔄 COROUTINES
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
