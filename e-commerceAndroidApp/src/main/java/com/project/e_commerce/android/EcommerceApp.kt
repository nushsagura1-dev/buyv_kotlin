package com.project.e_commerce.android

import android.app.Application
import coil3.ImageLoader
import coil3.ComponentRegistry
import com.project.e_commerce.android.data.remote.CloudinaryConfig
import com.project.e_commerce.android.di.viewModelModule
// import com.project.e_commerce.android.presentation.utils.CloudinaryUriFetcher
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class EcommerceApp : Application() {
    
    companion object {
        lateinit var imageLoader: ImageLoader
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("CrashDebug", "EcommerceApp: onCreate started")
        try {
            android.util.Log.d("CrashDebug", "EcommerceApp: Initializing CloudinaryConfig")
            CloudinaryConfig.init(this)
            android.util.Log.d("CrashDebug", "EcommerceApp: CloudinaryConfig initialized")
            android.util.Log.d("CrashDebug", "EcommerceApp: Initializing Coil imageLoader")
            imageLoader = ImageLoader.Builder(this)
                // .components(componentRegistry) // Comment out custom components temporarily
                .build()
            android.util.Log.d("CrashDebug", "EcommerceApp: Coil imageLoader initialized")
            android.util.Log.d("CrashDebug", "EcommerceApp: Starting Koin")
            startKoin {
                androidContext(this@EcommerceApp)
                modules(
                    listOf(
                        viewModelModule,
                    )
                )
            }
            android.util.Log.d("CrashDebug", "EcommerceApp: Koin started successfully")
        } catch (e: Exception) {
            android.util.Log.e("CrashDebug", "EcommerceApp: Exception during init: ${e.message}", e)
        }
    }
}