package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.Comment

/**
 * Repository interface for Comment operations
 * Defines contract for comment-related data operations
 */
interface CommentRepository {
    
    /**
     * Add a comment to a post
     * @param postId ID of the post
     * @param content Comment text content
     * @param userId ID of the user adding the comment
     * @return Created Comment
     */
    suspend fun addComment(postId: String, content: String, userId: String): Comment
    
    /**
     * Get comments for a post with pagination
     * @param postId ID of the post
     * @param limit Maximum number of comments to fetch (default: 20)
     * @param offset Number of comments to skip for pagination (default: 0)
     * @return List of comments
     */
    suspend fun getComments(postId: String, limit: Int = 20, offset: Int = 0): List<Comment>
    
    /**
     * Delete a comment
     * @param postId ID of the post
     * @param commentId ID of the comment to delete
     * @param userId ID of the user deleting the comment (must be comment owner)
     */
    suspend fun deleteComment(postId: String, commentId: Int, userId: String)
    
    /**
     * Like a comment
     * @param commentId ID of the comment to like
     * @param userId ID of the user liking the comment
     */
    suspend fun likeComment(commentId: String, userId: String)
}
