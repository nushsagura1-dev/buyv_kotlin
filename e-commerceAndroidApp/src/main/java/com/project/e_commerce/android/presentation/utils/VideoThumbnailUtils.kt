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
            if (videoUrl.contains("cloudinary.com") && videoUrl.contains(".mp4")) {
                Log.d("VideoThumbnailUtils", "🎬 Processing Cloudinary video URL: $videoUrl")

                // Handle different Cloudinary URL formats
                when {
                    videoUrl.contains("/video/upload/") -> {
                        // Standard video upload path: /video/upload/v123456/path/file.mp4
                        val thumbnailUrl = videoUrl
                            .replace("/video/upload/", "/video/upload/so_0/")
                            .replace(".mp4", ".jpg")
                        Log.d("VideoThumbnailUtils", "✅ Generated thumbnail URL: $thumbnailUrl")
                        thumbnailUrl
                    }

                    videoUrl.contains("/upload/") -> {
                        // Generic upload path: /upload/v123456/path/file.mp4  
                        val thumbnailUrl = videoUrl
                            .replace("/upload/", "/upload/so_0/")
                            .replace(".mp4", ".jpg")
                        Log.d(
                            "VideoThumbnailUtils",
                            "✅ Generated thumbnail URL (generic): $thumbnailUrl"
                        )
                        thumbnailUrl
                    }

                    else -> {
                        // Try to insert so_0 before the filename
                        val lastSlashIndex = videoUrl.lastIndexOf('/')
                        if (lastSlashIndex != -1) {
                            val basePath = videoUrl.substring(0, lastSlashIndex + 1)
                            val fileName = videoUrl.substring(lastSlashIndex + 1)
                            val thumbnailUrl = "${basePath}so_0/$fileName".replace(".mp4", ".jpg")
                            Log.d(
                                "VideoThumbnailUtils",
                                "✅ Generated thumbnail URL (filename): $thumbnailUrl"
                            )
                            thumbnailUrl
                        } else {
                            Log.w(
                                "VideoThumbnailUtils",
                                "⚠️ Cannot process Cloudinary URL format: $videoUrl"
                            )
                            videoUrl
                        }
                    }
                }
            } else {
                Log.d(
                    "VideoThumbnailUtils",
                    "ℹ️ Not a Cloudinary video URL, returning as-is: $videoUrl"
                )
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
     * 2. Generated video thumbnail from Cloudinary (re-enabled)
     * 3. Existing thumbnail URL (fallback)
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

        // Priority 2: Generate video thumbnail from Cloudinary (re-enabled!)
        val videoThumbnail = generateVideoThumbnail(videoUrl)
        if (!videoThumbnail.isNullOrBlank()) {
            Log.d("VideoThumbnailUtils", "✅ Using generated video thumbnail: $videoThumbnail")
            return videoThumbnail
        }

        // Priority 3: Use existing thumbnail URL (fallback)
        if (!fallbackUrl.isNullOrBlank()) {
            Log.d("VideoThumbnailUtils", "✅ Using existing thumbnail URL: $fallbackUrl")
            return fallbackUrl
        }

        Log.d("VideoThumbnailUtils", "❌ No thumbnail available")
        return null
    }
}
