package com.project.e_commerce.domain.model

/**
 * Post domain model
 * Represents a social post/reel in the application
 */
data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String? = null,
    val caption: String = "",
    val mediaUrl: String = "", // Video or image URL
    val mediaType: MediaType = MediaType.IMAGE,
    val thumbnailUrl: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val bookmarksCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class MediaType {
    IMAGE,
    VIDEO
}
