package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.Sound

/**
 * Repository interface for sound/music operations.
 */
interface SoundRepository {
    /** Get sounds with optional search, genre filter, and pagination. */
    suspend fun getSounds(
        search: String? = null,
        genre: String? = null,
        featured: Boolean? = null,
        limit: Int = 30,
        offset: Int = 0
    ): List<Sound>

    /** Get all available genres. */
    suspend fun getGenres(): List<String>

    /** Get trending sounds. */
    suspend fun getTrendingSounds(limit: Int = 20): List<Sound>

    /** Get a specific sound by UID. */
    suspend fun getSoundByUid(soundUid: String): Sound

    /** Increment usage count for a sound. */
    suspend fun incrementUsage(soundUid: String)
}
