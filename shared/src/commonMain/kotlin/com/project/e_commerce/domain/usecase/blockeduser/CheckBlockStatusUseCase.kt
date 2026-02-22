package com.project.e_commerce.domain.usecase.blockeduser

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.BlockedUserRepository

/**
 * Use case to check if a user is blocked.
 */
class CheckBlockStatusUseCase(
    private val blockedUserRepository: BlockedUserRepository
) {
    suspend operator fun invoke(userUid: String): Result<Boolean> {
        if (userUid.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        return try {
            val isBlocked = blockedUserRepository.isUserBlocked(userUid)
            Result.Success(isBlocked)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to check block status"))
        }
    }
}
