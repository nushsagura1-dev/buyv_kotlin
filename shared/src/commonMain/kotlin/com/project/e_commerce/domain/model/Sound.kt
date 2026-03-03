package com.project.e_commerce.domain.model

/**
 * Sound domain model.
 * Represents a sound/music track for Reels creation.
 */
data class Sound(
    val id: Int = 0,
    val uid: String = "",
    val title: String = "Original Sound",
    val artist: String = "Unknown",
    val audioUrl: String = "",
    val coverImageUrl: String? = null,
    val duration: Double = 0.0,
    val genre: String? = null,
    val usageCount: Int = 0,
    val isFeatured: Boolean = false,
    val createdAt: Long = 0L // Timestamp in milliseconds
) {
    /** Returns true when this is a valid networked sound (not a local fallback). */
    val isValid: Boolean get() = uid.isNotBlank() && audioUrl.isNotBlank()
}
