package com.project.e_commerce.data.remote.api

import com.project.e_commerce.data.remote.dto.SoundDto
import com.project.e_commerce.data.remote.dto.SoundUsageResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Sound API Service.
 * Handles sound/music library endpoints for Reels creation.
 *
 * Endpoints:
 *  - GET  /api/sounds                    → list sounds (search, genre, featured, pagination)
 *  - GET  /api/sounds/genres             → list genres
 *  - GET  /api/sounds/trending           → trending sounds
 *  - GET  /api/sounds/{uid}              → get a specific sound
 *  - POST /api/sounds/{uid}/use          → increment usage count (auth)
 */
class SoundApiService(private val httpClient: HttpClient) {

    private val baseUrl = "api/sounds"

    /**
     * Get available sounds with optional filters.
     */
    suspend fun getSounds(
        search: String? = null,
        genre: String? = null,
        featured: Boolean? = null,
        limit: Int = 30,
        offset: Int = 0
    ): List<SoundDto> {
        return httpClient.get(baseUrl) {
            search?.let { parameter("search", it) }
            genre?.let { parameter("genre", it) }
            featured?.let { parameter("featured", it) }
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
    }

    /**
     * Get all available sound genres.
     */
    suspend fun getGenres(): List<String> {
        return httpClient.get("$baseUrl/genres").body()
    }

    /**
     * Get trending sounds (most used).
     */
    suspend fun getTrendingSounds(limit: Int = 20): List<SoundDto> {
        return httpClient.get("$baseUrl/trending") {
            parameter("limit", limit)
        }.body()
    }

    /**
     * Get a specific sound by UID.
     */
    suspend fun getSound(soundUid: String): SoundDto {
        return httpClient.get("$baseUrl/$soundUid").body()
    }

    /**
     * Increment usage count when a sound is used in a Reel.
     */
    suspend fun incrementUsage(soundUid: String): SoundUsageResponseDto {
        return httpClient.post("$baseUrl/$soundUid/use").body()
    }
}
