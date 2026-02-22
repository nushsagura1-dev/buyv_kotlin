package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use case for deleting the current user's account.
 * 
 * This permanently deletes the user account and all associated data:
 * - Posts, reels, and photos
 * - Comments and likes  
 * - Follows and followers
 * - Orders and commissions
 * - Notifications
 * 
 * Required for App Store and Play Store compliance (GDPR/CCPA).
 * 
 * @param userRepository Repository for user operations
 */
class DeleteAccountUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Execute the account deletion.
     * 
     * @return Result<String> with success message or error
     */
    suspend operator fun invoke(): Result<String> {
        return try {
            val response = userRepository.deleteAccount()
            Result.Success(response.message)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
}
