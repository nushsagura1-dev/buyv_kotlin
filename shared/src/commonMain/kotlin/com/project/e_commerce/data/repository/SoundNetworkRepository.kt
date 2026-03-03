package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.SoundApiService
import com.project.e_commerce.data.remote.dto.SoundDto
import com.project.e_commerce.domain.model.Sound
import com.project.e_commerce.domain.repository.SoundRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException

/**
 * Network implementation of SoundRepository.
 */
class SoundNetworkRepository(
    private val soundApiService: SoundApiService
) : SoundRepository {

    override suspend fun getSounds(
        search: String?,
        genre: String?,
        featured: Boolean?,
        limit: Int,
        offset: Int
    ): List<Sound> {
        return soundApiService.getSounds(search, genre, featured, limit, offset).map { it.toDomain() }
    }

    override suspend fun getGenres(): List<String> {
        return soundApiService.getGenres()
    }

    override suspend fun getTrendingSounds(limit: Int): List<Sound> {
        return soundApiService.getTrendingSounds(limit).map { it.toDomain() }
    }

    override suspend fun getSoundByUid(soundUid: String): Sound {
        return try {
            soundApiService.getSound(soundUid).toDomain()
        } catch (e: SerializationException) {
            // Partial/missing fields from backend — return safe default instead of crash
            Sound(
                id = 0, uid = soundUid, title = "Original Sound",
                artist = "Unknown", audioUrl = "", coverImageUrl = null,
                duration = 0.0, genre = null, usageCount = 0,
                isFeatured = false, createdAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    override suspend fun incrementUsage(soundUid: String) {
        soundApiService.incrementUsage(soundUid)
    }

    private fun SoundDto.toDomain(): Sound {
        return Sound(
            id = id,
            uid = uid,
            title = title,
            artist = artist,
            audioUrl = audioUrl,
            coverImageUrl = coverImageUrl,
            duration = duration,
            genre = genre,
            usageCount = usageCount,
            isFeatured = isFeatured,
            createdAt = parseTimestamp(createdAt)
        )
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            kotlinx.datetime.Instant.parse(timestamp).toEpochMilliseconds()
        } catch (e: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }
    }
}
