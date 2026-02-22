package com.project.e_commerce.domain.usecase.post

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.PostRepository

/**
 * Use case to like a post
 * 
 * Business Rules:
 * - Post ID must not be empty
 * - User ID must not be empty
 * - User cannot like the same post multiple times (idempotent)
 * 
 * OWASP Security:
 * - Validates input IDs
 * - Backend verifies authentication
 */
class LikePostUseCase(
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
            postRepository.likePost(postId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to like post"))
        }
    }
}
