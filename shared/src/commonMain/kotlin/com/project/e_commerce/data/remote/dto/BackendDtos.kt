package com.project.e_commerce.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs (Data Transfer Objects) pour les réponses de l'API Backend.
 * 
 * Ces DTOs correspondent aux schémas Pydantic du backend (schemas.py).
 * Utilisent camelCase avec @SerialName pour mapper avec le backend qui utilise snake_case.
 */

/**
 * Réponse d'authentification (login/register).
 */
@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("user") val user: UserDto,
    @SerialName("refresh_token") val refreshToken: String? = null
)

/**
 * Utilisateur (UserOut du backend).
 */
@Serializable
data class UserDto(
    @SerialName("id") val id: String, // uid du backend
    @SerialName("email") val email: String,
    @SerialName("username") val username: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("role") val role: String = "user", // user, promoter, admin
    @SerialName("followersCount") val followersCount: Int = 0,
    @SerialName("followingCount") val followingCount: Int = 0,
    @SerialName("reelsCount") val reelsCount: Int = 0,
    @SerialName("isVerified") val isVerified: Boolean = false,
    @SerialName("createdAt") val createdAt: String, // ISO DateTime string
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("interests") val interests: List<String> = emptyList(),
    @SerialName("settings") val settings: Map<String, String>? = null
)

/**
 * Requête de login.
 */
@Serializable
data class LoginRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

/**
 * Requête Google Sign-In.
 */
@Serializable
data class GoogleSignInRequestDto(
    @SerialName("id_token") val idToken: String
)

/**
 * Requête de réinitialisation de mot de passe.
 */
@Serializable
data class PasswordResetRequestDto(
    @SerialName("email") val email: String
)

/**
 * Réponse de réinitialisation de mot de passe.
 */
@Serializable
data class PasswordResetResponseDto(
    @SerialName("message") val message: String
)

/**
 * Requête de création d'utilisateur.
 */
@Serializable
data class UserCreateDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("username") val username: String,
    @SerialName("displayName") val displayName: String
)

/**
 * Requête de création de post (PostCreate du backend).
 */
@Serializable
data class PostCreateRequest(
    @SerialName("type") val type: String, // "reel", "product", "photo"
    @SerialName("mediaUrl") val mediaUrl: String,
    @SerialName("caption") val caption: String? = null,
    @SerialName("additionalData") val additionalData: Map<String, String>? = null
)

/**
 * Post (Reel) du backend - correspondant à PostOut.
 */
@Serializable
data class PostDto(
    @SerialName("id") val id: String,
    @SerialName("userId") val userId: String,
    
    // User data flattened
    @SerialName("username") val username: String,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("userProfileImage") val userProfileImage: String? = null,
    @SerialName("isUserVerified") val isUserVerified: Boolean = false,
    
    @SerialName("type") val type: String,
    @SerialName("videoUrl") val videoUrl: String,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerialName("caption") val caption: String? = null,
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("commentsCount") val commentsCount: Int = 0,
    @SerialName("sharesCount") val sharesCount: Int = 0,
    @SerialName("viewsCount") val viewsCount: Int = 0,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("isLiked") val isLiked: Boolean = false,
    @SerialName("isBookmarked") val isBookmarked: Boolean = false,
    @SerialName("duration") val duration: Double = 0.0,
    @SerialName("metadata") val metadata: Map<String, String>? = null,
    @SerialName("marketplaceProductUid") val marketplaceProductUid: String? = null
)

// -------------------- Order Create Request DTOs --------------------

/**
 * Order creation request body — replaces Map<String, Any?> for kotlinx.serialization.
 */
@Serializable
data class OrderCreateRequest(
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("items") val items: List<OrderItemCreateRequest>,
    @SerialName("status") val status: String = "pending",
    @SerialName("subtotal") val subtotal: Double,
    @SerialName("shipping") val shipping: Double,
    @SerialName("tax") val tax: Double,
    @SerialName("total") val total: Double,
    @SerialName("shipping_address") val shippingAddress: ShippingAddressRequest? = null,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("promoter_id") val promoterId: String? = null,
    @SerialName("payment_intent_id") val paymentIntentId: String? = null
)

@Serializable
data class OrderItemCreateRequest(
    @SerialName("id") val id: String? = null,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("product_image") val productImage: String,
    @SerialName("price") val price: Double,
    @SerialName("quantity") val quantity: Int,
    @SerialName("size") val size: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("attributes") val attributes: Map<String, String>? = null,
    @SerialName("is_promoted_product") val isPromotedProduct: Boolean = false,
    @SerialName("promoter_id") val promoterId: String? = null
)

@Serializable
data class ShippingAddressRequest(
    @SerialName("fullName") val fullName: String,
    @SerialName("address") val address: String,
    @SerialName("city") val city: String,
    @SerialName("state") val state: String,
    @SerialName("zipCode") val zipCode: String,
    @SerialName("country") val country: String,
    @SerialName("phone") val phone: String
)

// -------------------- Order Response DTOs --------------------

/**
 * Commande (Order) du backend.
 */
@Serializable
data class OrderDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("userId") val userId: Int = 0,
    @SerialName("orderNumber") val orderNumber: String,
    @SerialName("items") val items: List<OrderItemDto>,
    @SerialName("status") val status: String,
    @SerialName("subtotal") val subtotal: Double,
    @SerialName("shipping") val shipping: Double,
    @SerialName("tax") val tax: Double,
    @SerialName("total") val total: Double,
    @SerialName("shippingAddress") val shippingAddress: AddressDto? = null,
    @SerialName("paymentMethod") val paymentMethod: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

/**
 * Item de commande.
 */
@Serializable
data class OrderItemDto(
    @SerialName("productId") val productId: String,
    @SerialName("productName") val productName: String,
    @SerialName("productImage") val productImage: String,
    @SerialName("price") val price: Double,
    @SerialName("quantity") val quantity: Int,
    @SerialName("size") val size: String? = null,
    @SerialName("color") val color: String? = null
)

/**
 * Adresse de livraison.
 */
@Serializable
data class AddressDto(
    @SerialName("id") val id: String? = null,
    @SerialName("fullName") val fullName: String = "",
    @SerialName("address") val address: String = "",
    @SerialName("city") val city: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("zipCode") val zipCode: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("phone") val phone: String = ""
)

/**
 * Statistiques utilisateur.
 */
@Serializable
data class UserStatsDto(
    @SerialName("followersCount") val followersCount: Int,
    @SerialName("followingCount") val followingCount: Int,
    @SerialName("reelsCount") val reelsCount: Int,
    @SerialName("productsCount") val productsCount: Int,
    @SerialName("totalLikes") val totalLikes: Int,
    @SerialName("savedPostsCount") val savedPostsCount: Int
)

/**
 * Information simplifiée d'utilisateur pour les listes followers/following.
 */
@Serializable
data class UserFollowInfoDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
    @SerialName("isVerified") val isVerified: Boolean = false
)

/**
 * Statut de suivi entre deux utilisateurs.
 */
@Serializable
data class FollowingStatusDto(
    @SerialName("isFollowing") val isFollowing: Boolean,
    @SerialName("isFollowedBy") val isFollowedBy: Boolean
)

/**
 * Post utilisateur (pour getUserPosts).
 */
@Serializable
data class UserPostDto(
    @SerialName("id") val id: String, // post uid
    @SerialName("userId") val userId: String,
    @SerialName("type") val type: String, // 'reel' | 'product' | 'photo'
    @SerialName("videoUrl") val videoUrl: String, // media_url
    @SerialName("caption") val caption: String? = null,
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("commentsCount") val commentsCount: Int = 0,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

/**
 * Mise à jour du profil utilisateur.
 */
@Serializable
data class UserUpdateDto(
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("interests") val interests: List<String>? = null,
    @SerialName("settings") val settings: Map<String, String>? = null
)

/**
 * Réponse générique de succès.
 */
@Serializable
data class MessageResponseDto(
    @SerialName("message") val message: String
)

/**
 * Réponse de comptage.
 */
@Serializable
data class CountResponseDto(
    @SerialName("count") val count: Int
)

/**
 * Delete account response DTO
 */
@Serializable
data class DeleteAccountResponseDto(
    @SerialName("message") val message: String,
    @SerialName("deleted_user_id") val deletedUserId: String
)

/**
 * Comment Response DTO (CommentOut from backend)
 */
@Serializable
data class CommentDto(
    @SerialName("id") val id: Int,
    @SerialName("userId") val userId: String,
    @SerialName("username") val username: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("userProfileImage") val userProfileImage: String? = null,
    @SerialName("postId") val postId: String,
    @SerialName("content") val content: String,
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("isLiked") val isLiked: Boolean = false,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

/**
 * Create Comment Request DTO (CommentCreate from backend)
 */
@Serializable
data class CommentCreateRequest(
    @SerialName("content") val content: String
)

// ==================== Admin DTOs ====================

/**
 * Admin Dashboard statistics (from /api/admin/dashboard/stats).
 */
@Serializable
data class AdminDashboardStatsDto(
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("verified_users") val verifiedUsers: Int = 0,
    @SerialName("new_users_today") val newUsersToday: Int = 0,
    @SerialName("new_users_this_week") val newUsersThisWeek: Int = 0,
    @SerialName("total_posts") val totalPosts: Int = 0,
    @SerialName("total_reels") val totalReels: Int = 0,
    @SerialName("total_products") val totalProducts: Int = 0,
    @SerialName("total_comments") val totalComments: Int = 0,
    @SerialName("total_likes") val totalLikes: Int = 0,
    @SerialName("total_follows") val totalFollows: Int = 0,
    @SerialName("total_orders") val totalOrders: Int = 0,
    @SerialName("pending_orders") val pendingOrders: Int = 0,
    @SerialName("total_commissions") val totalCommissions: Int = 0,
    @SerialName("pending_commissions") val pendingCommissions: Int = 0,
    @SerialName("total_revenue") val totalRevenue: Double = 0.0,
    @SerialName("pending_withdrawals") val pendingWithdrawals: Int = 0,
    @SerialName("pending_withdrawals_amount") val pendingWithdrawalsAmount: Double = 0.0
)

/**
 * Recent user (admin dashboard).
 */
@Serializable
data class AdminRecentUserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String
)

/**
 * Recent order (admin dashboard).
 */
@Serializable
data class AdminRecentOrderDto(
    @SerialName("id") val id: Int,
    @SerialName("buyer_email") val buyerEmail: String,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String
)

/**
 * User in admin user list.
 */
@Serializable
data class AdminUserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("reels_count") val reelsCount: Int = 0,
    @SerialName("created_at") val createdAt: String
)

/**
 * Request body for user actions (verify/unverify).
 */
@Serializable
data class AdminUserActionDto(
    @SerialName("user_ids") val userIds: List<String>
)

/**
 * Report (from /api/admin/reports).
 */
@Serializable
data class AdminReportDto(
    @SerialName("id") val id: Int,
    @SerialName("reporterUid") val reporterUid: String,
    @SerialName("reporterUsername") val reporterUsername: String,
    @SerialName("targetType") val targetType: String,
    @SerialName("targetId") val targetId: String,
    @SerialName("reason") val reason: String,
    @SerialName("description") val description: String? = null,
    @SerialName("status") val status: String,
    @SerialName("adminNotes") val adminNotes: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

/**
 * Request to update report status.
 */
@Serializable
data class AdminReportActionDto(
    @SerialName("status") val status: String,
    @SerialName("adminNotes") val adminNotes: String? = null
)

/**
 * Report statistics.
 */
@Serializable
data class AdminReportStatsDto(
    @SerialName("total") val total: Int = 0,
    @SerialName("pending") val pending: Int = 0,
    @SerialName("reviewed") val reviewed: Int = 0,
    @SerialName("resolved") val resolved: Int = 0,
    @SerialName("dismissed") val dismissed: Int = 0
)

// ==================== Comment Like DTOs ====================

/**
 * Response from toggling a comment like.
 */
@Serializable
data class CommentLikeResponseDto(
    @SerialName("is_liked") val isLiked: Boolean,
    @SerialName("likes_count") val likesCount: Int
)

// ==================== Blocked Users DTOs ====================

/**
 * Blocked user info (BlockedUserOut from backend).
 */
@Serializable
data class BlockedUserDto(
    @SerialName("id") val id: Int,
    @SerialName("blockedUid") val blockedUid: String,
    @SerialName("blockedUsername") val blockedUsername: String,
    @SerialName("blockedDisplayName") val blockedDisplayName: String,
    @SerialName("blockedProfileImage") val blockedProfileImage: String? = null,
    @SerialName("createdAt") val createdAt: String
)

/**
 * Request to block a user.
 */
@Serializable
data class BlockUserRequestDto(
    @SerialName("userId") val userId: String
)

/**
 * Block status check response.
 */
@Serializable
data class BlockStatusDto(
    @SerialName("is_blocked") val isBlocked: Boolean
)

// ==================== Sound DTOs ====================

/**
 * Sound from backend (SoundOut).
 */
@Serializable
data class SoundDto(
    @SerialName("id") val id: Int,
    @SerialName("uid") val uid: String,
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("audioUrl") val audioUrl: String,
    @SerialName("coverImageUrl") val coverImageUrl: String? = null,
    @SerialName("duration") val duration: Double = 0.0,
    @SerialName("genre") val genre: String? = null,
    @SerialName("usageCount") val usageCount: Int = 0,
    @SerialName("isFeatured") val isFeatured: Boolean = false,
    @SerialName("createdAt") val createdAt: String
)

/**
 * Response from incrementing sound usage.
 */
@Serializable
data class SoundUsageResponseDto(
    @SerialName("message") val message: String,
    @SerialName("usage_count") val usageCount: Int
)

// ==================== Tracking DTOs ====================

/**
 * Request to track a reel view.
 */
@Serializable
data class TrackViewRequestDto(
    @SerialName("reel_id") val reelId: String,
    @SerialName("promoter_uid") val promoterUid: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("viewer_uid") val viewerUid: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("watch_duration") val watchDuration: Int? = null,
    @SerialName("completion_rate") val completionRate: Double? = null
)

/**
 * Request to track an affiliate click.
 */
@Serializable
data class TrackClickRequestDto(
    @SerialName("reel_id") val reelId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("promoter_uid") val promoterUid: String,
    @SerialName("viewer_uid") val viewerUid: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("device_info") val deviceInfo: String? = null
)

/**
 * Request to track a conversion.
 */
@Serializable
data class TrackConversionRequestDto(
    @SerialName("order_id") val orderId: String,
    @SerialName("click_session_id") val clickSessionId: String
)

/**
 * Response from tracking endpoints.
 */
@Serializable
data class TrackingResponseDto(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String,
    @SerialName("tracking_id") val trackingId: Int? = null
)

// ==================== Password Reset Confirm DTO ====================

/**
 * Request to confirm password reset with token.
 */
@Serializable
data class PasswordResetConfirmDto(
    @SerialName("token") val token: String,
    @SerialName("new_password") val newPassword: String
)
