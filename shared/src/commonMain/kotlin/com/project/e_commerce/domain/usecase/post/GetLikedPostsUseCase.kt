package com.project.e_commerce.domain.usecase.post

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Post
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.PostRepository

/**
 * Use case to get user's liked posts
 * 
 * Business Rules:
 * - User ID must not be empty
 * - Returns posts that user has liked
 * - Useful for "Liked Posts" screen
 * 
 * OWASP Security:
 * - Validates user ID
 * - Backend verifies authentication
 */
class GetLikedPostsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(userId: String): Result<List<Post>> {
        // Validation
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        // Execute
        return try {
            val posts = postRepository.getLikedPosts(userId)
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to fetch liked posts"))
        }
    }
}
