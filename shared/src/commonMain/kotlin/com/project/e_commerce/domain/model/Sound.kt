package com.project.e_commerce.domain.model

/**
 * Sound domain model.
 * Represents a sound/music track for Reels creation.
 */
data class Sound(
    val id: Int,
    val uid: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverImageUrl: String?,
    val duration: Double,
    val genre: String?,
    val usageCount: Int,
    val isFeatured: Boolean,
    val createdAt: Long // Timestamp in milliseconds
)
