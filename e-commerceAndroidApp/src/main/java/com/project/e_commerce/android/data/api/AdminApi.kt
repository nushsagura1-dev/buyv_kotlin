package com.project.e_commerce.android.data.api

import com.project.e_commerce.android.data.model.admin.*
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

interface AdminApi {
    
    // Authentication
    @POST("auth/admin/login")
    suspend fun adminLogin(@Body request: AdminLoginRequest): AdminLoginResponse
    
    // Dashboard
    @GET("api/admin/dashboard/stats")
    suspend fun getDashboardStats(
        @Header("Authorization") token: String
    ): DashboardStatsResponse
    
    @GET("api/admin/dashboard/recent-users")
    suspend fun getRecentUsers(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 10
    ): List<RecentUserResponse>
    
    @GET("api/admin/dashboard/recent-orders")
    suspend fun getRecentOrders(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 10
    ): List<RecentOrderResponse>
    
    // User Management
    @GET("api/admin/users")
    suspend fun getUsers(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null,
        @Query("is_verified") isVerified: Boolean? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<UserManagementResponse>
    
    @POST("api/admin/users/verify")
    suspend fun verifyUsers(
        @Header("Authorization") token: String,
        @Body request: UserActionRequest
    ): MessageResponse
    
    @POST("api/admin/users/unverify")
    suspend fun unverifyUsers(
        @Header("Authorization") token: String,
        @Body request: UserActionRequest
    ): MessageResponse
    
    @DELETE("api/admin/users/{user_uid}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("user_uid") userUid: String
    ): MessageResponse
    
    // Order Management
    @GET("api/orders/admin/all")
    suspend fun getAllOrders(
        @Header("Authorization") token: String
    ): List<com.project.e_commerce.android.data.model.Order>
    
    @GET("api/orders/admin/status")
    suspend fun getOrdersByStatus(
        @Header("Authorization") token: String,
        @Query("status") status: String
    ): List<com.project.e_commerce.android.data.model.Order>
    
    @PATCH("api/orders/{order_id}/status")
    suspend fun updateOrderStatus(
        @Header("Authorization") token: String,
        @Path("order_id") orderId: Int,
        @Body request: StatusUpdateRequest
    ): MessageResponse

    // Product Management
    @GET("api/v1/marketplace/products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("sort_by") sortBy: String? = null
    ): AdminProductsResponse

    @PUT("api/v1/admin/marketplace/products/{product_id}")
    suspend fun updateProduct(
        @Header("Authorization") token: String,
        @Path("product_id") productId: String,
        @Body request: ProductUpdateRequest
    ): ProductUpdateResponse

    @DELETE("api/v1/admin/marketplace/products/{product_id}")
    suspend fun deleteProduct(
        @Header("Authorization") token: String,
        @Path("product_id") productId: String
    ): MessageResponse
    
    // Commission Management
    @GET("api/commissions/admin/all")
    suspend fun getAllCommissions(
        @Header("Authorization") token: String
    ): List<com.project.e_commerce.domain.model.Commission>

    @GET("api/commissions/admin/status")
    suspend fun getCommissionsByStatus(
        @Header("Authorization") token: String,
        @Query("status") status: String
    ): List<com.project.e_commerce.domain.model.Commission>

    @PATCH("api/commissions/{commission_id}/status")
    suspend fun updateCommissionStatus(
        @Header("Authorization") token: String,
        @Path("commission_id") commissionId: Int,
        @Body request: StatusUpdateRequest
    ): MessageResponse

    // CJ Dropshipping Import
    @GET("api/v1/admin/cj/search")
    suspend fun searchCJProducts(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1
    ): CJSearchResponse

    @POST("api/v1/admin/cj/import")
    suspend fun importCJProduct(
        @Header("Authorization") token: String,
        @Body request: CJImportRequest
    ): CJImportResponse

    @POST("api/v1/admin/cj/sync/{product_id}")
    suspend fun syncCJProduct(
        @Header("Authorization") token: String,
        @Path("product_id") productId: String
    ): MessageResponse

    // Post Management
    @GET("api/admin/posts")
    suspend fun getAdminPosts(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null,
        @Query("post_type") postType: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<AdminPostResponse>

    @DELETE("api/admin/posts/{post_uid}")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Path("post_uid") postUid: String
    ): MessageResponse

    // Comment Management
    @GET("api/admin/comments")
    suspend fun getAdminComments(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<AdminCommentResponse>

    @DELETE("api/admin/comments/{comment_id}")
    suspend fun deleteComment(
        @Header("Authorization") token: String,
        @Path("comment_id") commentId: Int
    ): MessageResponse

    // Follow Stats
    @GET("api/admin/follows/stats")
    suspend fun getFollowStats(
        @Header("Authorization") token: String
    ): FollowStatsResponse

    // Notification Management
    @GET("api/admin/notifications")
    suspend fun getAdminNotifications(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<AdminNotificationResponse>

    @POST("api/admin/notifications/send")
    suspend fun sendNotification(
        @Header("Authorization") token: String,
        @Body request: SendNotificationRequest
    ): MessageResponse

    // Category Management
    @GET("api/v1/marketplace/categories")
    suspend fun getCategories(
        @Query("parent_id") parentId: String? = null
    ): List<AdminCategoryResponse>

    @POST("api/v1/admin/marketplace/categories")
    suspend fun createCategory(
        @Header("Authorization") token: String,
        @Body request: CategoryCreateRequest
    ): AdminCategoryResponse

    @PUT("api/v1/admin/marketplace/categories/{category_id}")
    suspend fun updateCategory(
        @Header("Authorization") token: String,
        @Path("category_id") categoryId: String,
        @Body request: CategoryUpdateRequest
    ): AdminCategoryResponse

    @DELETE("api/v1/admin/marketplace/categories/{category_id}")
    suspend fun deleteCategory(
        @Header("Authorization") token: String,
        @Path("category_id") categoryId: String
    ): MessageResponse

    // Affiliate Sales Management (Admin)
    @GET("api/v1/admin/sales")
    suspend fun getAdminAffiliateSales(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<AdminAffiliateSaleResponse>

    @PATCH("api/v1/admin/sales/{sale_id}/status")
    suspend fun updateAffiliateSaleStatus(
        @Header("Authorization") token: String,
        @Path("sale_id") saleId: String,
        @Body request: SaleStatusUpdateRequest
    ): AdminAffiliateSaleResponse
}

// Request models
data class AdminLoginRequest(
    val email: String,
    val password: String
)

data class UserActionRequest(
    val user_ids: List<String>
)

data class StatusUpdateRequest(
    val status: String
)

data class ProductUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val short_description: String? = null,
    val main_image_url: String? = null,
    val images: List<String>? = null,
    val original_price: Double? = null,
    val selling_price: Double? = null,
    val commission_rate: Double? = null,
    val commission_amount: Double? = null,
    val commission_type: String? = null,
    val category_id: String? = null,
    val tags: List<String>? = null,
    val status: String? = null,
    val is_featured: Boolean? = null,
    val is_choice: Boolean? = null
)

data class ProductUpdateResponse(
    val id: String,
    val name: String,
    val description: String?,
    @SerializedName("main_image_url") val mainImageUrl: String?,
    val images: List<String>,
    @SerializedName("original_price") val originalPrice: Double,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("commission_rate") val commissionRate: Double,
    @SerializedName("category_name") val categoryName: String?,
    @SerializedName("is_featured") val isFeatured: Boolean,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("total_sales") val totalSales: Int
)

// Response models
data class AdminLoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val admin: AdminInfo
)

data class AdminInfo(
    val id: Int,
    val username: String,
    val email: String,
    val role: String
)

data class DashboardStatsResponse(
    val total_users: Int,
    val verified_users: Int,
    val new_users_today: Int,
    val new_users_this_week: Int,
    val total_posts: Int,
    val total_reels: Int,
    val total_products: Int,
    val total_comments: Int,
    val total_likes: Int,
    val total_follows: Int,
    val total_orders: Int,
    val pending_orders: Int,
    val total_commissions: Int,
    val pending_commissions: Int,
    val total_revenue: Double,
    val pending_withdrawals: Int,
    val pending_withdrawals_amount: Double
)

data class RecentUserResponse(
    val id: String,
    val username: String,
    val email: String,
    val display_name: String,
    val is_verified: Boolean,
    val created_at: String
)

data class RecentOrderResponse(
    val id: Int,
    val buyer_email: String,
    val total_amount: Double,
    val status: String,
    val created_at: String
)

data class UserManagementResponse(
    val id: String,
    val username: String,
    val email: String,
    val display_name: String,
    val is_verified: Boolean,
    val followers_count: Int,
    val following_count: Int,
    val reels_count: Int,
    val created_at: String
)

data class MessageResponse(
    val message: String,
    val count: Int? = null
)

// CJ Dropshipping Models
data class CJProduct(
    val product_id: String,
    val product_name: String,
    val product_image: String,
    val sell_price: Double,
    val original_price: Double? = null,
    val product_url: String? = null,
    val category_name: String? = null,
    val description: String? = null,
    val variants: List<CJVariant>? = null
)

data class CJVariant(
    val variant_id: String,
    val variant_name: String,
    val variant_sku: String? = null,
    val sell_price: Double,
    val stock: Int? = null
)

data class CJSearchResponse(
    val products: List<CJProduct>,
    val total: Int,
    val page: Int,
    val page_size: Int
)

data class CJImportRequest(
    val cj_product_id: String,
    val cj_variant_id: String? = null,
    val commission_rate: Double = 10.0,
    val category_id: String? = null,
    val custom_description: String? = null,
    val selling_price: Double? = null
)

data class CJImportResponse(
    val id: String,
    val name: String,
    val main_image_url: String?,
    val selling_price: Double,
    val original_price: Double,
    val commission_rate: Double,
    val cj_product_id: String?,
    val status: String,
    val message: String? = null
)

// Admin Post Management Models
data class AdminPostResponse(
    val id: Int,
    val uid: String,
    val username: String,
    val user_uid: String,
    val type: String,
    val caption: String?,
    val media_url: String,
    val thumbnail_url: String?,
    val likes_count: Int,
    val comments_count: Int,
    val views_count: Int,
    val is_promoted: Boolean,
    val created_at: String
)

// Admin Comment Management Models
data class AdminCommentResponse(
    val id: Int,
    val username: String,
    val user_uid: String,
    val post_uid: String,
    val content: String,
    val created_at: String
)

// Admin Follow Stats Models
data class FollowStatsResponse(
    val total_follows: Int,
    val new_follows_today: Int,
    val new_follows_this_week: Int,
    val top_followed_users: List<TopFollowedUser>
)

data class TopFollowedUser(
    val uid: String,
    val username: String,
    val display_name: String,
    val followers_count: Int
)

// Admin Notification Models
data class AdminNotificationResponse(
    val id: Int,
    val user_uid: String,
    val username: String,
    val title: String,
    val body: String,
    val type: String,
    val is_read: Boolean,
    val created_at: String
)

data class SendNotificationRequest(
    val title: String,
    val body: String,
    val type: String = "admin_broadcast",
    val target_user_uids: List<String>? = null
)

// Category Management Models
data class AdminCategoryResponse(
    val id: String,
    val name: String,
    val name_ar: String?,
    val slug: String,
    val icon_url: String?,
    val parent_id: String?,
    val display_order: Int,
    val is_active: Boolean,
    val created_at: String
)

data class CategoryCreateRequest(
    val name: String,
    val name_ar: String? = null,
    val slug: String,
    val icon_url: String? = null,
    val parent_id: String? = null,
    val display_order: Int = 0,
    val is_active: Boolean = true
)

data class CategoryUpdateRequest(
    val name: String? = null,
    val name_ar: String? = null,
    val icon_url: String? = null,
    val display_order: Int? = null,
    val is_active: Boolean? = null
)

// Affiliate Sales Admin Models
data class AdminAffiliateSaleResponse(
    val id: String,
    val order_id: String,
    val product_id: String,
    val promotion_id: String?,
    val buyer_user_id: String,
    val promoter_user_id: String?,
    val sale_amount: Double,
    val product_price: Double,
    val quantity: Int,
    val commission_rate: Double,
    val commission_amount: Double,
    val commission_status: String,
    val paid_at: String?,
    val payment_reference: String?,
    val created_at: String
)

data class SaleStatusUpdateRequest(
    val status: String,
    val payment_reference: String? = null,
    val payment_notes: String? = null
)
