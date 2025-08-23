package com.project.e_commerce.android.data.remote

import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager

object CloudinaryConfig {
    const val CLOUD_NAME = "dwtbxzkst"
    const val CLOUDINARY_API_KEY = "137652538693452"
    const val CLOUDINARY_API_SECRET = "UyOMGSySXjNLDrYsB2HwzK9Sa0w"
    const val UPLOAD_PRESET = "Ecommerce_BuyV"
    
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
