package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.PostApiService
import com.project.e_commerce.data.remote.dto.PostCreateRequest
import com.project.e_commerce.domain.model.MediaType
import com.project.e_commerce.domain.model.Post
import com.project.e_commerce.domain.repository.PostRepository
import com.project.e_commerce.data.remote.dto.PostDto
import kotlinx.datetime.Clock

/**
 * Network implementation of PostRepository
 * Handles post-related operations via backend API
 */
class PostNetworkRepository(
    private val postApiService: PostApiService
) : PostRepository {
    
    override suspend fun createPost(type: String, mediaUrl: String, caption: String?): Post {
        val request = PostCreateRequest(
            type = type,
            mediaUrl = mediaUrl,
            caption = caption
        )
        val postDto = postApiService.createPost(request)
        return postDto.toDomain()
    }
    
    override suspend fun likePost(postId: String, userId: String) {
        postApiService.likePost(postId)
    }
    
    override suspend fun unlikePost(postId: String, userId: String) {
        // Call DELETE endpoint to unlike post
        postApiService.unlikePost(postId)
    }
    
    override suspend fun bookmarkPost(postId: String, userId: String) {
        postApiService.bookmarkPost(postId)
    }
    
    override suspend fun unbookmarkPost(postId: String, userId: String) {
        // Call DELETE endpoint to unbookmark post
        postApiService.unbookmarkPost(postId)
    }
    
    override suspend fun deletePost(postId: String, userId: String) {
        // Call DELETE endpoint to delete post (backend checks ownership)
        postApiService.deletePost(postId)
    }
    
    override suspend fun getLikedPosts(userId: String): List<Post> {
        val postDtos = postApiService.getPostsLikedByUser(userId)
        return postDtos.map { it.toDomain() }
    }
    
    override suspend fun getBookmarkedPosts(userId: String): List<Post> {
        val postDtos = postApiService.getBookmarkedPosts()
        return postDtos.map { it.toDomain() }
    }
    
    override suspend fun checkPostLikeStatus(postId: String, userId: String): Boolean {
        return try {
            val post = postApiService.getPostById(postId)
            post.isLiked
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun checkPostBookmarkStatus(postId: String, userId: String): Boolean {
        return try {
            val post = postApiService.getPostById(postId)
            post.isBookmarked
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extension function to convert PostDto to Post domain model
     */
    private fun PostDto.toDomain(): Post {
        return Post(
            id = id,
            userId = userId,
            username = username,
            userProfileImage = userProfileImage,
            caption = caption ?: "",
            mediaUrl = videoUrl,
            mediaType = if (type == "reel") MediaType.VIDEO else MediaType.IMAGE,
            thumbnailUrl = thumbnailUrl,
            likesCount = likesCount,
            commentsCount = commentsCount,
            bookmarksCount = 0, // Not in DTO
            isLiked = isLiked,
            isBookmarked = isBookmarked,
            createdAt = parseTimestamp(createdAt),
            updatedAt = parseTimestamp(updatedAt)
        )
    }
    
    /**
     * Parse ISO 8601 timestamp to Long (milliseconds)
     */
    override suspend fun getOrCreatePostForMarketplaceProduct(productUuid: String): Post {
        val postDto = postApiService.getPostByMarketplaceProduct(productUuid)
        return postDto.toDomain()
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // Simplified parsing, should use proper date parser
            Clock.System.now().toEpochMilliseconds()
        } catch (e: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }
    }
}
