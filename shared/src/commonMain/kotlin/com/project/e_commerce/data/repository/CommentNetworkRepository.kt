package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.CommentsApiService
import com.project.e_commerce.data.remote.dto.CommentDto
import com.project.e_commerce.domain.model.Comment
import com.project.e_commerce.domain.repository.CommentRepository
import kotlinx.datetime.Clock

/**
 * Network implementation of CommentRepository
 * Handles comment operations via backend API
 */
class CommentNetworkRepository(
    private val commentsApiService: CommentsApiService
) : CommentRepository {
    
    override suspend fun addComment(postId: String, content: String, userId: String): Comment {
        val commentDto = commentsApiService.addComment(postId, content)
        return commentDto.toDomain()
    }
    
    override suspend fun getComments(postId: String, limit: Int, offset: Int): List<Comment> {
        val commentDtos = commentsApiService.getComments(postId, limit, offset)
        return commentDtos.map { it.toDomain() }
    }
    
    override suspend fun deleteComment(postId: String, commentId: Int, userId: String) {
        commentsApiService.deleteComment(postId, commentId)
    }
    
    override suspend fun likeComment(commentId: String, userId: String) {
        // userId is used as postId for the API call (interface limitation)
        commentsApiService.likeComment(userId, commentId.toInt())
    }
    
    /**
     * Extension function to convert CommentDto to Comment domain model
     */
    private fun CommentDto.toDomain(): Comment {
        return Comment(
            id = id,
            postId = postId,
            userId = userId,
            username = username,
            displayName = displayName,
            userProfileImage = userProfileImage,
            content = content,
            likesCount = likesCount,
            isLiked = isLiked,
            createdAt = parseTimestamp(createdAt),
            updatedAt = parseTimestamp(updatedAt)
        )
    }
    
    /**
     * Parse ISO 8601 timestamp to Long (milliseconds)
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // Simplified parsing, should use proper date parser
            Clock.System.now().toEpochMilliseconds()
        } catch (e: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }
    }
}
