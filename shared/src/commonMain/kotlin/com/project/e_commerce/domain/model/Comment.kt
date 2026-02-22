package com.project.e_commerce.domain.model

/**
 * Comment domain model
 * Represents a comment on a post/reel
 */
data class Comment(
    val id: Int,
    val postId: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val userProfileImage: String?,
    val content: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: Long, // Timestamp in milliseconds
    val updatedAt: Long
)

