package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.Post

/**
 * Repository interface for Post operations
 * Defines contract for post-related data operations
 */
interface PostRepository {
    
    /**
     * Create a new post/reel
     * @param type Type of post ("reel", "product", "photo")
     * @param mediaUrl URL of the uploaded media (video/image)
     * @param caption Optional caption/description
     * @return Created post
     */
    suspend fun createPost(type: String, mediaUrl: String, caption: String?): Post
    
    /**
     * Like a post
     * @param postId ID of the post to like
     * @param userId ID of the user liking the post
     */
    suspend fun likePost(postId: String, userId: String)
    
    /**
     * Unlike a post
     * @param postId ID of the post to unlike
     * @param userId ID of the user unliking the post
     */
    suspend fun unlikePost(postId: String, userId: String)
    
    /**
     * Bookmark/save a post
     * @param postId ID of the post to bookmark
     * @param userId ID of the user bookmarking the post
     */
    suspend fun bookmarkPost(postId: String, userId: String)
    
    /**
     * Unbookmark/unsave a post
     * @param postId ID of the post to unbookmark
     * @param userId ID of the user unbookmarking the post
     */
    suspend fun unbookmarkPost(postId: String, userId: String)
    
    /**
     * Delete a post
     * @param postId ID of the post to delete
     * @param userId ID of the user deleting the post (must be owner)
     */
    suspend fun deletePost(postId: String, userId: String)
    
    /**
     * Get all posts liked by a user
     * @param userId ID of the user
     * @return List of liked posts
     */
    suspend fun getLikedPosts(userId: String): List<Post>
    
    /**
     * Get all posts bookmarked by a user
     * @param userId ID of the user
     * @return List of bookmarked posts
     */
    suspend fun getBookmarkedPosts(userId: String): List<Post>
    
    /**
     * Check if user has liked a specific post
     * @param postId ID of the post
     * @param userId ID of the user
     * @return true if user has liked the post
     */
    suspend fun checkPostLikeStatus(postId: String, userId: String): Boolean
    
    /**
     * Check if user has bookmarked a specific post
     * @param postId ID of the post
     * @param userId ID of the user
     * @return true if user has bookmarked the post
     */
    suspend fun checkPostBookmarkStatus(postId: String, userId: String): Boolean

    /**
     * Get or create shadow post for marketplace product.
     * Returns the post UID linked to this marketplace product.
     */
    suspend fun getOrCreatePostForMarketplaceProduct(productUuid: String): Post
}
