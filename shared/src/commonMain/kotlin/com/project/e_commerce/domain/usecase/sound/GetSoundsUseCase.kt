package com.project.e_commerce.domain.usecase.sound

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.Sound
import com.project.e_commerce.domain.repository.SoundRepository

/**
 * Use case to get a list of sounds with optional filters.
 */
class GetSoundsUseCase(
    private val soundRepository: SoundRepository
) {
    suspend operator fun invoke(
        search: String? = null,
        genre: String? = null,
        featured: Boolean? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Sound>> {
        return try {
            val sounds = soundRepository.getSounds(search, genre, featured, limit, offset)
            Result.Success(sounds)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to load sounds"))
        }
    }
}
