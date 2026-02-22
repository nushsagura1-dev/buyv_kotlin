package com.project.e_commerce.domain.usecase.sound

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.Sound
import com.project.e_commerce.domain.repository.SoundRepository

/**
 * Use case to get trending sounds.
 */
class GetTrendingSoundsUseCase(
    private val soundRepository: SoundRepository
) {
    suspend operator fun invoke(limit: Int = 10): Result<List<Sound>> {
        return try {
            val sounds = soundRepository.getTrendingSounds(limit)
            Result.Success(sounds)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to load trending sounds"))
        }
    }
}
