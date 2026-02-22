package com.project.e_commerce.domain.usecase.blockeduser

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.BlockedUser
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.BlockedUserRepository

/**
 * Use case to block a user.
 */
class BlockUserUseCase(
    private val blockedUserRepository: BlockedUserRepository
) {
    suspend operator fun invoke(userId: String): Result<BlockedUser> {
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        return try {
            val blocked = blockedUserRepository.blockUser(userId)
            Result.Success(blocked)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to block user"))
        }
    }
}
