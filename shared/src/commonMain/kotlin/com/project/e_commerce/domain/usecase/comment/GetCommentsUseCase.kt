package com.project.e_commerce.domain.usecase.comment

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Comment
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CommentRepository

/**
 * Use case to get comments for a post/reel
 * 
 * Business Rules:
 * - Post ID must not be empty
 * - Returns paginated list of comments
 * - Comments ordered by creation date (newest first)
 * 
 * OWASP Security:
 * - Validates post ID
 * - Supports pagination to prevent large data loads
 */
class GetCommentsUseCase(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(
        postId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Comment>> {
        // Validation
        if (postId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Post ID cannot be empty"))
        }
        
        if (limit <= 0 || limit > 100) {
            return Result.Error(ApiError.ValidationError("Limit must be between 1 and 100"))
        }
        
        if (offset < 0) {
            return Result.Error(ApiError.ValidationError("Offset cannot be negative"))
        }
        
        // Execute
        return try {
            val comments = commentRepository.getComments(postId, limit, offset)
            Result.Success(comments)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to fetch comments"))
        }
    }
}
