package com.project.e_commerce.android.domain.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val bio: String = "",
    val phone: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class UserPost(
    val id: String = "",
    val userId: String = "",
    val type: String = "REEL",  // Changed from PostType to String to match Firestore
    val title: String = "",
    val description: String = "",
    val mediaUrl: String = "",
    val thumbnailUrl: String? = null,
    val images: List<String> = emptyList(),  // Added images field for reel thumbnails
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val viewsCount: Int = 0,
    val isPublished: Boolean = true,
    val createdAt: Any = System.currentTimeMillis(),  // Changed from Long to Any to handle Firestore Timestamp
    val updatedAt: Any = System.currentTimeMillis()   // Changed from Long to Any to handle Firestore Timestamp
)

data class UserProduct(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val originalPrice: Double? = null,
    val images: List<String> = emptyList(),
    val category: String = "",
    val stockQuantity: Int = 0,
    val isPublished: Boolean = true,
    val likesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class UserInteraction(
    val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val targetType: InteractionType = InteractionType.LIKE,
    val createdAt: Long = System.currentTimeMillis()
)

// New: Following relationship model
data class FollowRelationship(
    val id: String = "",
    val followerId: String = "",  // User who is following
    val followedId: String = "",  // User being followed
    val createdAt: Long = System.currentTimeMillis()
)

// New: Following status for UI
data class FollowingStatus(
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val isMutual: Boolean = false
)

// New: User follow model for UI
data class UserFollowModel(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val isFollowingMe: Boolean = false,
    val isIFollow: Boolean = false
)

enum class PostType {
    REEL, PRODUCT
}

enum class InteractionType {
    LIKE, BOOKMARK, FOLLOW
}
