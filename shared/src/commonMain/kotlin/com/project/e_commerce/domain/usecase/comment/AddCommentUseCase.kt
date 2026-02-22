package com.project.e_commerce.domain.usecase.comment

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Comment
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CommentRepository

/**
 * Use case to add a comment to a post/reel
 * 
 * Business Rules:
 * - Post ID must not be empty
 * - User ID must not be empty
 * - Comment text must not be empty
 * - Comment text max length: 500 characters
 * 
 * OWASP Security:
 * - Validates input lengths
 * - Sanitizes comment text
 * - Backend verifies authentication
 */
class AddCommentUseCase(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(
        postId: String,
        content: String,
        userId: String
    ): Result<Comment> {
        // Validation
        if (postId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Post ID cannot be empty"))
        }
        
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        if (content.isBlank()) {
            return Result.Error(ApiError.ValidationError("Comment text cannot be empty"))
        }
        
        if (content.length > 500) {
            return Result.Error(ApiError.ValidationError("Comment text cannot exceed 500 characters"))
        }
        
        // Sanitize input (remove leading/trailing whitespace)
        val sanitizedText = content.trim()
        
        // Execute
        return try {
            val comment = commentRepository.addComment(postId, sanitizedText, userId)
            Result.Success(comment)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to add comment"))
        }
    }
}
