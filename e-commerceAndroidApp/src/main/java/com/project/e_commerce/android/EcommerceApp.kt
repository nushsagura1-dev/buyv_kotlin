package com.project.e_commerce.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.project.e_commerce.android.data.remote.CloudinaryConfig
import com.project.e_commerce.android.di.viewModelModule
import com.project.e_commerce.android.domain.repository.NotificationRepository
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

        try {
            // Initialize Firebase first
            android.util.Log.e("ECOMMERCE_APP", "🔥 Initializing Firebase")
            FirebaseApp.initializeApp(this)
            android.util.Log.e("ECOMMERCE_APP", "✅ Firebase initialized")

            android.util.Log.e("ECOMMERCE_APP", "🔧 Initializing CloudinaryConfig")
            CloudinaryConfig.init(this)
            android.util.Log.e("ECOMMERCE_APP", "✅ CloudinaryConfig initialized")

            // Manually set up Coil ImageLoader
            android.util.Log.e("ECOMMERCE_APP", "🖼️ Setting up Coil ImageLoader for Coil 3.x")

            val imageLoader = ImageLoader.Builder(this)
                .logger(DebugLogger())
                .crossfade(true)
                .build()

            SingletonImageLoader.setSafe { imageLoader }
            android.util.Log.e("ECOMMERCE_APP", "✅ Coil ImageLoader set up successfully")

            android.util.Log.e("ECOMMERCE_APP", "🔧 Starting Koin")
            startKoin {
                androidContext(this@EcommerceApp)
                modules(
                    listOf(
                        viewModelModule,
                    )
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

                    // Check if user is authenticated
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        // Update token in repository
                        applicationScope.launch {
                            try {
                                val notificationRepository =
                                    org.koin.java.KoinJavaComponent.getKoin()
                                        .get<NotificationRepository>()
                                val result =
                                    notificationRepository.updateFCMToken(currentUser.uid, token)

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
                        }
                    } else {
                        android.util.Log.w(
                            "ECOMMERCE_APP",
                            "⚠️ User not authenticated, FCM token not saved"
                        )
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