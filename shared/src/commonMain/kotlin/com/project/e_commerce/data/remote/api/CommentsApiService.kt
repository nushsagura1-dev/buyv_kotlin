package com.project.e_commerce.data.remote.api

import com.project.e_commerce.data.remote.dto.CommentCreateRequest
import com.project.e_commerce.data.remote.dto.CommentDto
import com.project.e_commerce.data.remote.dto.CommentLikeResponseDto
import com.project.e_commerce.data.remote.dto.MessageResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Comments API Service
 * Handles all comment-related HTTP requests to the backend
 * 
 * Endpoints:
 * - POST /comments/{postId} - Add comment to post
 * - GET /comments/{postId} - Get comments for post
 * - DELETE /comments/{postId}/{commentId} - Delete comment
 */
class CommentsApiService(private val httpClient: HttpClient) {
    
    private val baseUrl = "comments"
    
    /**
     * Add a comment to a post
     * @param postId UID of the post
     * @param content Comment text content
     * @return CommentDto with created comment details
     */
    suspend fun addComment(postId: String, content: String): CommentDto {
        val response: HttpResponse = httpClient.post("$baseUrl/$postId") {
            contentType(ContentType.Application.Json)
            setBody(CommentCreateRequest(content = content))
        }
        if (response.status.value !in 200..299) {
            throw Exception("Failed to add comment (HTTP ${response.status.value})")
        }
        return response.body()
    }
    
    /**
     * Get comments for a post with pagination
     * @param postId UID of the post
     * @param limit Number of comments to fetch (default: 20, max: 100)
     * @param offset Number of comments to skip (default: 0)
     * @return List of CommentDto
     */
    suspend fun getComments(
        postId: String,
        limit: Int = 20,
        offset: Int = 0
    ): List<CommentDto> {
        val response: HttpResponse = httpClient.get("$baseUrl/$postId") {
            parameter("limit", limit)
            parameter("offset", offset)
        }
        // Return empty list for non-2xx responses (e.g. 404 "Post not found")
        if (response.status.value !in 200..299) {
            return emptyList()
        }
        return response.body()
    }
    
    /**
     * Delete a comment
     * @param postId UID of the post
     * @param commentId ID of the comment to delete
     * @return MessageResponseDto with status
     */
    suspend fun deleteComment(postId: String, commentId: Int): MessageResponseDto {
        return httpClient.delete("$baseUrl/$postId/$commentId").body()
    }
    
    /**
     * Toggle like on a comment
     * POST /comments/{postId}/{commentId}/like
     */
    suspend fun likeComment(postId: String, commentId: Int): CommentLikeResponseDto {
        return httpClient.post("$baseUrl/$postId/$commentId/like").body()
    }
}
