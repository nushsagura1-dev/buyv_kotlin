package com.project.e_commerce.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.util.DebugLogger
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toOkioPath
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.project.e_commerce.android.data.remote.CloudinaryConfig
import com.project.e_commerce.data.remote.ApiEnvironment
import com.project.e_commerce.android.di.viewModelModule
import com.project.e_commerce.android.di.marketplaceModule
import com.project.e_commerce.android.di.cameraModule
import com.project.e_commerce.android.domain.repository.NotificationRepository
import com.project.e_commerce.android.presentation.utils.VideoPlayerCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class EcommerceApp : Application() {

    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Use more visible logging
        android.util.Log.e("ECOMMERCE_APP", "🚀 EcommerceApp: onCreate started")

        // ── API Environment ───────────────────────────────────────────────────
        // Always use the deployed Railway backend (for both debug tests and release)
        ApiEnvironment.current = ApiEnvironment.Environment.PRODUCTION
        android.util.Log.e("ECOMMERCE_APP", "🌐 API URL: ${ApiEnvironment.baseUrl}")

        try {
            // Initialize Firebase first
            android.util.Log.e("ECOMMERCE_APP", "🔥 Initializing Firebase")
            FirebaseApp.initializeApp(this)
            android.util.Log.e("ECOMMERCE_APP", "✅ Firebase initialized")

            // Initialize EmojiCompat for consistent emoji rendering on all Android versions
            EmojiCompat.init(BundledEmojiCompatConfig(this))
            android.util.Log.e("ECOMMERCE_APP", "✅ EmojiCompat initialized")

            android.util.Log.e("ECOMMERCE_APP", "🔧 Initializing CloudinaryConfig")
            CloudinaryConfig.init(this)
            android.util.Log.e("ECOMMERCE_APP", "✅ CloudinaryConfig initialized")
            
            // Initialize Video Player Cache
            android.util.Log.e("ECOMMERCE_APP", "🎥 Initializing ExoPlayer Cache")
            VideoPlayerCache.initialize(this)
            android.util.Log.e("ECOMMERCE_APP", "✅ ExoPlayer Cache initialized")

            // Set up Coil ImageLoader with disk and memory cache
            android.util.Log.e("ECOMMERCE_APP", "🖼️ Setting up Coil ImageLoader with caching for Coil 3.x")

            val imageLoader = ImageLoader.Builder(this)
                .logger(DebugLogger())
                .crossfade(true)
                .memoryCache {
                    MemoryCache.Builder()
                        // 25% of available memory for image cache
                        .maxSizePercent(this, 0.25)
                        .strongReferencesEnabled(true)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache").toOkioPath())
                        // 250 MB disk cache for images
                        .maxSizeBytes(250L * 1024 * 1024)
                        .build()
                }
                .build()

            SingletonImageLoader.setSafe { imageLoader }
            android.util.Log.e("ECOMMERCE_APP", "✅ Coil ImageLoader configured: Memory 25%, Disk 250MB")

            android.util.Log.e("ECOMMERCE_APP", "🔧 Starting Koin")
            startKoin {
                androidContext(this@EcommerceApp)
                modules(
                    com.project.e_commerce.di.sharedModules + viewModelModule + marketplaceModule + cameraModule
                )
            }
            android.util.Log.e("ECOMMERCE_APP", "✅ Koin started successfully")

            // Initialize FCM token after Koin is set up
            initializeFCMToken()

        } catch (e: Exception) {
            android.util.Log.e("ECOMMERCE_APP", "❌ Exception during init: ${e.message}", e)
        }

        android.util.Log.e("ECOMMERCE_APP", "🎉 EcommerceApp initialization completed")
    }

    private fun initializeFCMToken() {
        android.util.Log.e("ECOMMERCE_APP", "🔔 Initializing FCM token")

        applicationScope.launch {
            try {
                // Get FCM token
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        android.util.Log.w(
                            "ECOMMERCE_APP",
                            "❌ FCM token fetch failed",
                            task.exception
                        )
                        return@addOnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result
                    android.util.Log.d("ECOMMERCE_APP", "✅ FCM token retrieved: $token")

                    // Check if user is authenticated via backend
                    val currentUserProvider = org.koin.java.KoinJavaComponent.getKoin().get<com.project.e_commerce.data.local.CurrentUserProvider>()
                    applicationScope.launch {
                        val currentUserId = currentUserProvider.getCurrentUserId()
                        if (currentUserId != null) {
                            // Update token in repository
                            try {
                                val notificationRepository =
                                    org.koin.java.KoinJavaComponent.getKoin()
                                        .get<NotificationRepository>()
                                val result =
                                    notificationRepository.updateFCMToken(currentUserId, token)

                                if (result.isSuccess) {
                                    android.util.Log.d(
                                        "ECOMMERCE_APP",
                                        "✅ FCM token updated in Firestore"
                                    )
                                } else {
                                    android.util.Log.e(
                                        "ECOMMERCE_APP",
                                        "❌ Failed to update FCM token: ${result.exceptionOrNull()}"
                                    )
                                }
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "ECOMMERCE_APP",
                                    "❌ Error updating FCM token in repository",
                                    e
                                )
                            }
                        } else {
                            android.util.Log.w(
                                "ECOMMERCE_APP",
                                "⚠️ User not authenticated, FCM token not saved"
                            )
                        }
                    }
                }

                // Set up FCM token refresh listener
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("ECOMMERCE_APP", "✅ Subscribed to 'all_users' topic")
                        } else {
                            android.util.Log.w(
                                "ECOMMERCE_APP",
                                "❌ Failed to subscribe to 'all_users' topic",
                                task.exception
                            )
                        }
                    }

            } catch (e: Exception) {
                android.util.Log.e("ECOMMERCE_APP", "❌ Error in FCM initialization", e)
            }
        }
    }
}