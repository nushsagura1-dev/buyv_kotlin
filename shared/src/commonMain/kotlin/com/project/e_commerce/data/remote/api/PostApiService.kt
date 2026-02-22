package com.project.e_commerce.data.remote.api

import com.project.e_commerce.data.remote.dto.PostDto
import com.project.e_commerce.data.remote.dto.PostCreateRequest
import com.project.e_commerce.data.remote.dto.CountResponseDto
import com.project.e_commerce.data.remote.dto.MessageResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Post API service
 * Handles social post/product listing endpoints
 */
class PostApiService(private val httpClient: HttpClient) {
    
    /**
     * Create a new post/reel
     * POST /posts/
     */
    suspend fun createPost(request: PostCreateRequest): PostDto {
        return httpClient.post("posts/") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    /**
     * Get user feed (all posts)
     * GET /posts/feed?skip={skip}&limit={limit}
     */
    suspend fun getFeed(skip: Int = 0, limit: Int = 20): List<PostDto> {
        return httpClient.get("posts/feed") {
            parameter("skip", skip)
            parameter("limit", limit)
        }.body()
    }
    
    /**
     * Get specific post by UID
     * GET /posts/{post_uid}
     */
    suspend fun getPostById(postId: String): PostDto {
        return httpClient.get("posts/$postId").body()
    }
    
    /**
     * Like/unlike a post
     * POST /posts/{post_uid}/like
     */
    suspend fun likePost(postId: String): MessageResponseDto {
        return httpClient.post("posts/$postId/like").body()
    }
    
    /**
     * Unlike a post (explicit)
     * DELETE /posts/{post_uid}/like
     */
    suspend fun unlikePost(postId: String): MessageResponseDto {
        return httpClient.delete("posts/$postId/like").body()
    }
    
    /**
     * Bookmark/unbookmark a post
     * POST /posts/{post_uid}/bookmark
     */
    suspend fun bookmarkPost(postId: String): MessageResponseDto {
        return httpClient.post("posts/$postId/bookmark").body()
    }
    
    /**
     * Unbookmark a post (explicit)
     * DELETE /posts/{post_uid}/bookmark
     */
    suspend fun unbookmarkPost(postId: String): MessageResponseDto {
        return httpClient.delete("posts/$postId/bookmark").body()
    }
    
    /**
     * Delete a post
     * DELETE /posts/{post_uid}
     */
    suspend fun deletePost(postId: String): MessageResponseDto {
        return httpClient.delete("posts/$postId").body()
    }
    
    /**
     * Search posts by query
     * GET /posts/search?q={query}&post_type={type}&skip={skip}&limit={limit}
     */
    suspend fun searchPosts(
        query: String,
        postType: String? = null,
        skip: Int = 0,
        limit: Int = 20
    ): List<PostDto> {
        return httpClient.get("posts/search") {
            parameter("q", query)
            postType?.let { parameter("post_type", it) }
            parameter("skip", skip)
            parameter("limit", limit)
        }.body()
    }
    
    /**
     * Get posts by specific user
     * GET /posts/user/{user_id}?skip={skip}&limit={limit}
     */
    suspend fun getPostsByUser(
        userId: String,
        skip: Int = 0,
        limit: Int = 20
    ): List<PostDto> {
        return httpClient.get("posts/user/$userId") {
            parameter("skip", skip)
            parameter("limit", limit)
        }.body()
    }

    /**
     * Get bookmarked posts for current user
     * GET /posts/bookmarks?skip={skip}&limit={limit}
     */
    suspend fun getBookmarkedPosts(skip: Int = 0, limit: Int = 20): List<PostDto> {
        return httpClient.get("posts/bookmarks") {
            parameter("skip", skip)
            parameter("limit", limit)
        }.body()
    }
    
    /**
     * Get posts liked by a specific user
     * GET /posts/user/{user_id}/liked?skip={skip}&limit={limit}
     */
    suspend fun getPostsLikedByUser(
        userId: String,
        skip: Int = 0,
        limit: Int = 20
    ): List<PostDto> {
        return httpClient.get("posts/user/$userId/liked") {
            parameter("skip", skip)
            parameter("limit", limit)
        }.body()
    }

    /**
     * Get or create a shadow post for a marketplace product.
     * GET /posts/by-marketplace-product/{product_uuid}
     * Returns the PostDto linked to this marketplace product (creates one if needed).
     */
    suspend fun getPostByMarketplaceProduct(productUuid: String): PostDto {
        return httpClient.get("posts/by-marketplace-product/$productUuid").body()
    }
}
