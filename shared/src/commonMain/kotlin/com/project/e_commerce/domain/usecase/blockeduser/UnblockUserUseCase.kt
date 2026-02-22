package com.project.e_commerce.domain.usecase.blockeduser

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.BlockedUserRepository

/**
 * Use case to unblock a user.
 */
class UnblockUserUseCase(
    private val blockedUserRepository: BlockedUserRepository
) {
    suspend operator fun invoke(userUid: String): Result<Unit> {
        if (userUid.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        return try {
            blockedUserRepository.unblockUser(userUid)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to unblock user"))
        }
    }
}
