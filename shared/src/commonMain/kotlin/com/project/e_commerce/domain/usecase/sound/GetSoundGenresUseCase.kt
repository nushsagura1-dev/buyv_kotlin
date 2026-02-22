package com.project.e_commerce.domain.usecase.sound

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.SoundRepository

/**
 * Use case to get all available sound genres.
 */
class GetSoundGenresUseCase(
    private val soundRepository: SoundRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        return try {
            val genres = soundRepository.getGenres()
            Result.Success(genres)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to load sound genres"))
        }
    }
}
