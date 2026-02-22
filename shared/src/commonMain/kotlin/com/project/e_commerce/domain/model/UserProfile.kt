package com.project.e_commerce.domain.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Modèle de profil utilisateur partagé entre Android et iOS.
 * 
 * Représente le profil complet d'un utilisateur de l'application.
 * Validation des champs sensibles (username, bio) doit être effectuée avant utilisation.
 */
@Serializable
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "", // Max 50 caractères, alphanumerique uniquement
    val profileImageUrl: String? = null,
    val bio: String = "", // Max 500 caractères
    val phone: String = "",
    val role: String = "user", // user, promoter, admin
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val lastUpdated: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Modèle de publication utilisateur (Reel).
 */
@Serializable
data class UserPost(
    val id: String = "",
    val userId: String = "",
    val type: String = "REEL",
    val title: String = "",
    val description: String = "",
    val mediaUrl: String = "",
    val thumbnailUrl: String? = null,
    val images: List<String> = emptyList(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val viewsCount: Int = 0,
    val isPublished: Boolean = true,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Modèle de produit utilisateur.
 */
@Serializable
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
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Modèle d'interaction utilisateur (like, bookmark, follow).
 */
@Serializable
data class UserInteraction(
    val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val targetType: InteractionType = InteractionType.LIKE,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Relation de suivi entre utilisateurs.
 */
@Serializable
data class FollowRelationship(
    val id: String = "",
    val followerId: String = "", // Utilisateur qui suit
    val followedId: String = "", // Utilisateur suivi
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Statut de suivi pour l'UI.
 */
@Serializable
data class FollowingStatus(
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val isMutual: Boolean = false
)

/**
 * Modèle utilisateur pour l'affichage de la liste de followers/following.
 */
@Serializable
data class UserFollowModel(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val isFollowingMe: Boolean = false,
    val isIFollow: Boolean = false
)

/**
 * Type de publication.
 */
@Serializable
enum class PostType {
    REEL,
    PRODUCT
}

/**
 * Type d'interaction.
 */
@Serializable
enum class InteractionType {
    LIKE,
    BOOKMARK,
    FOLLOW
}
