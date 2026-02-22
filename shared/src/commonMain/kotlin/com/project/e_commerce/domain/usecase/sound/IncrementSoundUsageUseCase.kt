package com.project.e_commerce.domain.usecase.sound

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.SoundRepository

/**
 * Use case to increment usage count for a sound.
 */
class IncrementSoundUsageUseCase(
    private val soundRepository: SoundRepository
) {
    suspend operator fun invoke(soundUid: String): Result<Unit> {
        if (soundUid.isBlank()) {
            return Result.Error(ApiError.ValidationError("Sound ID cannot be empty"))
        }
        return try {
            soundRepository.incrementUsage(soundUid)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to increment sound usage"))
        }
    }
}
