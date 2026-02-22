package com.project.e_commerce.domain.usecase.post

import com.project.e_commerce.domain.repository.PostRepository

/**
 * Use case for deleting a post
 * 
 * Business rules:
 * - Post ID must not be empty
 * - User must be the owner (backend validates ownership)
 * - Backend will also:
 *   - Delete associated likes, bookmarks, comments
 *   - Decrement user's reels_count if type is "reel"
 */
class DeletePostUseCase(
    private val postRepository: PostRepository
) {
    
    /**
     * Delete a post
     * 
     * @param postId ID of the post to delete
     * @param userId ID of the user requesting deletion (must be post owner)
     * @throws IllegalArgumentException if postId or userId is empty
     * @throws Exception if backend returns error (e.g., not owner, post not found)
     */
    suspend operator fun invoke(postId: String, userId: String) {
        // Validate input
        require(postId.isNotBlank()) { "Post ID cannot be empty" }
        require(userId.isNotBlank()) { "User ID cannot be empty" }
        
        // Backend validates ownership and deletes post
        postRepository.deletePost(postId, userId)
    }
}
