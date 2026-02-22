package com.project.e_commerce.domain.usecase.sound

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.Sound
import com.project.e_commerce.domain.repository.SoundRepository

/**
 * Use case to get details for a specific sound.
 */
class GetSoundDetailsUseCase(
    private val soundRepository: SoundRepository
) {
    suspend operator fun invoke(soundUid: String): Result<Sound> {
        if (soundUid.isBlank()) {
            return Result.Error(ApiError.ValidationError("Sound ID cannot be empty"))
        }
        return try {
            val sound = soundRepository.getSoundByUid(soundUid)
            Result.Success(sound)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to load sound details"))
        }
    }
}
