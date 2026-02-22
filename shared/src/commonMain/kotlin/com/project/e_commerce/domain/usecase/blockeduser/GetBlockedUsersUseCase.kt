package com.project.e_commerce.domain.usecase.blockeduser

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.BlockedUser
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.BlockedUserRepository

/**
 * Use case to get the list of users blocked by the current user.
 */
class GetBlockedUsersUseCase(
    private val blockedUserRepository: BlockedUserRepository
) {
    suspend operator fun invoke(): Result<List<BlockedUser>> {
        return try {
            val blocked = blockedUserRepository.getBlockedUsers()
            Result.Success(blocked)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to get blocked users"))
        }
    }
}
