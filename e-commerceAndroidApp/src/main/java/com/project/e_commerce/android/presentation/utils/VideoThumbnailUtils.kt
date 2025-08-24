package com.project.e_commerce.android.presentation.utils

import android.util.Log

object VideoThumbnailUtils {
    
    /**
     * Generate a Cloudinary video thumbnail URL from a video URL.
     * Uses so_0 parameter to get the first frame (0 seconds) and converts to .jpg
     */
    fun generateVideoThumbnail(videoUrl: String?): String? {
        if (videoUrl.isNullOrBlank()) return null

        return try {
            if (videoUrl.contains("cloudinary.com")) {
                if (videoUrl.contains("/video/upload/")) {
                    // Replace the exact path segment to avoid double slashes
                    videoUrl.replace("/video/upload/", "/video/upload/so_0/") + ".jpg"
                } else if (videoUrl.contains("/upload/")) {
                    // Fallback for other upload paths
                    val baseUrl = videoUrl.substringBefore("upload/")
                    val restOfUrl = videoUrl.substringAfter("upload/")
                    "$baseUrl/upload/so_0/$restOfUrl.jpg"
                } else {
                    videoUrl
                }
            } else {
                videoUrl
            }
        } catch (e: Exception) {
            Log.e("VideoThumbnailUtils", "Error generating thumbnail: ${e.message}")
            videoUrl
        }
    }

    /**
     * Get the best thumbnail with priority:
     * 1. First image from images array (if available)
     * 2. Existing thumbnail URL (fallback - these are product images that work)
     * 3. Generated video thumbnail (temporarily disabled for testing)
     */
    fun getBestThumbnail(
        images: List<String>?,
        videoUrl: String?,
        fallbackUrl: String? = null
    ): String? {
        Log.d("VideoThumbnailUtils", "🔍 getBestThumbnail called:")
        Log.d("VideoThumbnailUtils", "  - Images: $images")
        Log.d("VideoThumbnailUtils", "  - Video URL: $videoUrl")
        Log.d("VideoThumbnailUtils", "  - Fallback URL: $fallbackUrl")

        // Priority 1: First image if available
        if (!images.isNullOrEmpty() && images.first().isNotBlank()) {
            Log.d("VideoThumbnailUtils", "✅ Using first image as thumbnail: ${images.first()}")
            return images.first()
        }

        // Priority 2: Use existing thumbnail URL (these are product images that work)
        if (!fallbackUrl.isNullOrBlank()) {
            Log.d("VideoThumbnailUtils", "✅ Using existing thumbnail URL: $fallbackUrl")
            return fallbackUrl
        }

        // Priority 3: Generate video thumbnail (temporarily disabled for testing)
        // val videoThumbnail = generateVideoThumbnail(videoUrl)
        // if (!videoThumbnail.isNullOrBlank()) {
        //     Log.d("VideoThumbnailUtils", "✅ Using generated video thumbnail: $videoThumbnail")
        //     return videoThumbnail
        // }

        Log.d("VideoThumbnailUtils", "❌ No thumbnail available")
        return null
    }
}
