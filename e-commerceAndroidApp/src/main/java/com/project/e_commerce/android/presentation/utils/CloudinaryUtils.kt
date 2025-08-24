package com.project.e_commerce.android.presentation.utils

/**
 * Utility class for handling Cloudinary URLs and operations.
 */
object CloudinaryUtils {
    
    /**
     * Check if a URL is a Cloudinary URL.
     */
    fun isCloudinaryUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains("res.cloudinary.com") || url.contains("cloudinary.com")
    }
    
    /**
     * Check if a URL is a Cloudinary image URL.
     */
    fun isCloudinaryImageUrl(url: String?): Boolean {
        if (!isCloudinaryUrl(url)) return false
        return url?.contains("/image/upload/") == true
    }
    
    /**
     * Check if a URL is a Cloudinary video URL.
     */
    fun isCloudinaryVideoUrl(url: String?): Boolean {
        if (!isCloudinaryUrl(url)) return false
        return url?.contains("/video/upload/") == true
    }
    
    /**
     * Transform a Cloudinary URL to ensure it's properly formatted for Coil.
     * This adds the https:// scheme if missing.
     */
    fun normalizeCloudinaryUrl(url: String): String {
        if (url.startsWith("//")) {
            return "https:$url"
        }
        if (!url.startsWith("http")) {
            return "https://$url"
        }
        return url
    }
}
