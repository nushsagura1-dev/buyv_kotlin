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
        
        // Initialize Cloudinary
        CloudinaryConfig.init(this)
        
        // Temporarily use default Coil configuration to test if custom fetcher is the issue
        imageLoader = ImageLoader.Builder(this)
            // .components(componentRegistry) // Comment out custom components temporarily
            .build()
        
        startKoin {
            androidContext(this@EcommerceApp)
            modules(
                listOf(
                    viewModelModule,
                )
            )
        }
    }
}