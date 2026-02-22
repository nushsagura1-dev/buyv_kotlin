package com.project.e_commerce.domain.usecase.post

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.PostRepository

/**
 * Use case to unlike a post
 * 
 * Business Rules:
 * - Post ID must not be empty
 * - User ID must not be empty
 * - Idempotent operation (no error if not liked)
 * 
 * OWASP Security:
 * - Validates input IDs
 * - Backend verifies authentication
 */
class UnlikePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, userId: String): Result<Unit> {
        // Validation
        if (postId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Post ID cannot be empty"))
        }
        
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        // Execute
        return try {
            postRepository.unlikePost(postId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to unlike post"))
        }
    }
}
