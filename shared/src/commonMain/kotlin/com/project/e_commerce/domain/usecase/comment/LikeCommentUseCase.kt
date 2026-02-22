package com.project.e_commerce.domain.usecase.comment

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CommentRepository

/**
 * Use case to like a comment
 * 
 * Business Rules:
 * - Comment ID must not be empty
 * - User ID must not be empty
 * - User can like/unlike comments
 * - Idempotent operation
 * 
 * OWASP Security:
 * - Validates input IDs
 * - Backend verifies authentication
 */
class LikeCommentUseCase(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(commentId: String, userId: String): Result<Unit> {
        // Validation
        if (commentId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Comment ID cannot be empty"))
        }
        
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        // Execute
        return try {
            commentRepository.likeComment(commentId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to like comment"))
        }
    }
}
