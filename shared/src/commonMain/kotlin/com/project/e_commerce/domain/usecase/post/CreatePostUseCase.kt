package com.project.e_commerce.domain.usecase.post

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Post
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.PostRepository

/**
 * Use case to create a new post/reel
 * 
 * Business Rules:
 * - Media URL must not be empty
 * - Type must be valid ("reel", "product", "photo")
 * - Caption is optional
 * 
 * OWASP Security:
 * - Validates input data
 * - Backend verifies authentication
 */
class CreatePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(type: String, mediaUrl: String, caption: String?): Result<Post> {
        // Validation
        if (type.isBlank()) {
            return Result.Error(ApiError.ValidationError("Post type cannot be empty"))
        }
        
        if (type !in listOf("reel", "product", "photo")) {
            return Result.Error(ApiError.ValidationError("Invalid post type. Must be 'reel', 'product', or 'photo'"))
        }
        
        if (mediaUrl.isBlank()) {
            return Result.Error(ApiError.ValidationError("Media URL cannot be empty"))
        }
        
        // Execute
        return try {
            val post = postRepository.createPost(type, mediaUrl, caption)
            Result.Success(post)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to create post"))
        }
    }
}
