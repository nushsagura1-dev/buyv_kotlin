package com.project.e_commerce.android.data.remote

import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.project.e_commerce.android.BuildConfig

object CloudinaryConfig {
    // ✅ Cloud name and upload preset are NOT secrets — safe to keep in code
    const val CLOUD_NAME = "dhzllfeno"
    const val UPLOAD_PRESET = "Ecommerce_BuyV"

    // ✅ Credentials loaded from BuildConfig (sourced from local.properties, never committed)
    val CLOUDINARY_API_KEY: String get() = BuildConfig.CLOUDINARY_API_KEY
    val CLOUDINARY_API_SECRET: String get() = BuildConfig.CLOUDINARY_API_SECRET
    
    // Folder structure for organized uploads
    object Folders {
        const val PRODUCTS = "ecommerce/products"
        const val REELS = "ecommerce/reels"
        const val USERS = "ecommerce/users"
        const val CATEGORIES = "ecommerce/categories"
        const val BANNERS = "ecommerce/banners"
    }
    
    // File size limits
    object Limits {
        const val IMAGE_MAX_SIZE = 10 * 1024 * 1024 // 10MB for images
        const val VIDEO_MAX_SIZE = 100 * 1024 * 1024 // 100MB for videos (optimized for reels)
    }
    
    // Allowed file formats
    object Formats {
        val IMAGES = arrayOf("jpg", "jpeg", "png", "webp", "gif")
        val VIDEOS = arrayOf("mp4", "mov", "avi", "mkv", "webm")
    }
    
    fun init(context: android.content.Context) {
        val config = HashMap<String, String>()
        config["cloud_name"] = CLOUD_NAME
        config["api_key"] = CLOUDINARY_API_KEY
        config["api_secret"] = CLOUDINARY_API_SECRET
        
        MediaManager.init(context, config)
    }
}
