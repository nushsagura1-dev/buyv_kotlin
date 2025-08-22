package com.project.e_commerce.android

import android.app.Application
import com.project.e_commerce.android.data.remote.CloudinaryConfig

import com.project.e_commerce.android.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class EcommerceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Cloudinary
        CloudinaryConfig.init(this)
        
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