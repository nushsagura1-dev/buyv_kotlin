package com.project.e_commerce.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.project.e_commerce.android.data.remote.CloudinaryConfig
import com.project.e_commerce.android.di.viewModelModule
import okhttp3.OkHttpClient
import com.project.e_commerce.android.presentation.utils.CloudinaryUriFetcher
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class EcommerceApp : Application() {
    
    override fun onCreate() {
        super.onCreate()

        // Use more visible logging
        android.util.Log.e("ECOMMERCE_APP", "🚀 EcommerceApp: onCreate started")

        try {
            android.util.Log.e("ECOMMERCE_APP", "🔧 Initializing CloudinaryConfig")
            CloudinaryConfig.init(this)
            android.util.Log.e("ECOMMERCE_APP", "✅ CloudinaryConfig initialized")

            // Manually set up Coil ImageLoader
            android.util.Log.e("ECOMMERCE_APP", "🖼️ Setting up Coil ImageLoader")
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

        } catch (e: Exception) {
            android.util.Log.e("ECOMMERCE_APP", "❌ Exception during init: ${e.message}", e)
        }

        android.util.Log.e("ECOMMERCE_APP", "🎉 EcommerceApp initialization completed")
    }
}