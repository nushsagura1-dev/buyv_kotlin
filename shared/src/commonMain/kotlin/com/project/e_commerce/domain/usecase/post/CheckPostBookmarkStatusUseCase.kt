package com.project.e_commerce.domain.usecase.post

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.PostRepository

/**
 * Use case to check if user has bookmarked a specific post
 * 
 * Business Rules:
 * - Post ID must not be empty
 * - User ID must not be empty
 * - Returns boolean status
 * - Used to show/hide bookmark button state in UI
 * 
 * OWASP Security:
 * - Validates input IDs
 */
class CheckPostBookmarkStatusUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, userId: String): Result<Boolean> {
        // Validation
        if (postId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Post ID cannot be empty"))
        }
        
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        // Execute
        return try {
            val isBookmarked = postRepository.checkPostBookmarkStatus(postId, userId)
            Result.Success(isBookmarked)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to check bookmark status"))
        }
    }
}
