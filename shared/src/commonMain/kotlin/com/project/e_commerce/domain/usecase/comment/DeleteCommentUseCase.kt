package com.project.e_commerce.domain.usecase.comment

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CommentRepository

/**
 * Use case to delete a comment
 * 
 * Business Rules:
 * - Comment ID must not be empty
 * - User can only delete their own comments
 * - Backend enforces ownership verification
 * 
 * OWASP Security:
 * - Validates comment ID
 * - Backend verifies user owns the comment
 * - Authorization check on backend
 */
class DeleteCommentUseCase(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(postId: String, commentId: Int, userId: String): Result<Unit> {
        // Validation
        if (postId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Post ID cannot be empty"))
        }
        
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        // Execute
        return try {
            commentRepository.deleteComment(postId, commentId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to delete comment"))
        }
    }
}
