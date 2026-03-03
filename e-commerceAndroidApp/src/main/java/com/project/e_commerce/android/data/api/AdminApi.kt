package com.project.e_commerce.android.data.api

import com.project.e_commerce.android.data.model.admin.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AdminApi(private val httpClient: HttpClient) {

    // Authentication
    suspend fun adminLogin(request: AdminLoginRequest): AdminLoginResponse =
        httpClient.post("auth/admin/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Dashboard
    suspend fun getDashboardStats(token: String): DashboardStatsResponse =
        httpClient.get("api/admin/dashboard/stats") {
            header(HttpHeaders.Authorization, token)
        }.body()

    suspend fun getRecentUsers(token: String, limit: Int = 10): List<RecentUserResponse> =
        httpClient.get("api/admin/dashboard/recent-users") {
            header(HttpHeaders.Authorization, token)
            parameter("limit", limit)
        }.body()

    suspend fun getRecentOrders(token: String, limit: Int = 10): List<RecentOrderResponse> =
        httpClient.get("api/admin/dashboard/recent-orders") {
            header(HttpHeaders.Authorization, token)
            parameter("limit", limit)
        }.body()

    // User Management
    suspend fun getUsers(
        token: String,
        search: String? = null,
        isVerified: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<UserManagementResponse> =
        httpClient.get("api/admin/users") {
            header(HttpHeaders.Authorization, token)
            search?.let { parameter("search", it) }
            isVerified?.let { parameter("is_verified", it) }
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun verifyUsers(token: String, request: UserActionRequest): MessageResponse =
        httpClient.post("api/admin/users/verify") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun unverifyUsers(token: String, request: UserActionRequest): MessageResponse =
        httpClient.post("api/admin/users/unverify") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun deleteUser(token: String, userUid: String): MessageResponse =
        httpClient.delete("api/admin/users/$userUid") {
            header(HttpHeaders.Authorization, token)
        }.body()

    // Order Management
    suspend fun getAllOrders(token: String): List<com.project.e_commerce.android.data.model.Order> =
        httpClient.get("api/orders/admin/all") {
            header(HttpHeaders.Authorization, token)
        }.body()

    suspend fun getOrdersByStatus(
        token: String,
        status: String
    ): List<com.project.e_commerce.android.data.model.Order> =
        httpClient.get("api/orders/admin/status") {
            header(HttpHeaders.Authorization, token)
            parameter("status", status)
        }.body()

    suspend fun updateOrderStatus(
        token: String,
        orderId: Int,
        request: StatusUpdateRequest
    ): MessageResponse =
        httpClient.patch("api/orders/$orderId/status") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Product Management
    suspend fun getProducts(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null,
        sortBy: String? = null
    ): AdminProductsResponse =
        httpClient.get("api/v1/marketplace/products") {
            parameter("page", page)
            parameter("limit", limit)
            search?.let { parameter("search", it) }
            sortBy?.let { parameter("sort_by", it) }
        }.body()

    suspend fun updateProduct(
        token: String,
        productId: String,
        request: ProductUpdateRequest
    ): ProductUpdateResponse =
        httpClient.put("api/v1/admin/marketplace/products/$productId") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun deleteProduct(token: String, productId: String): MessageResponse =
        httpClient.delete("api/v1/admin/marketplace/products/$productId") {
            header(HttpHeaders.Authorization, token)
        }.body()

    // Commission Management
    suspend fun getAllCommissions(token: String): List<com.project.e_commerce.domain.model.Commission> =
        httpClient.get("api/commissions/admin/all") {
            header(HttpHeaders.Authorization, token)
        }.body()

    suspend fun getCommissionsByStatus(
        token: String,
        status: String
    ): List<com.project.e_commerce.domain.model.Commission> =
        httpClient.get("api/commissions/admin/status") {
            header(HttpHeaders.Authorization, token)
            parameter("status", status)
        }.body()

    suspend fun updateCommissionStatus(
        token: String,
        commissionId: Int,
        request: StatusUpdateRequest
    ): MessageResponse =
        httpClient.patch("api/commissions/$commissionId/status") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // CJ Dropshipping Import
    suspend fun searchCJProducts(
        token: String,
        query: String,
        category: String? = null,
        page: Int = 1,
        warehouse: String? = null,
        shippingCountry: String? = null,
        sortBy: String? = null
    ): CJSearchResponse =
        httpClient.get("api/v1/admin/cj/search") {
            header(HttpHeaders.Authorization, token)
            parameter("query", query)
            category?.let { parameter("category", it) }
            parameter("page", page)
            warehouse?.let { parameter("warehouse", it) }
            shippingCountry?.let { parameter("shipping_to", it) }
            sortBy?.let { parameter("sort_by", it) }
        }.body()

    suspend fun importCJProduct(token: String, request: CJImportRequest): CJImportResponse =
        httpClient.post("api/v1/admin/cj/import") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun syncCJProduct(token: String, productId: String): MessageResponse =
        httpClient.post("api/v1/admin/cj/sync/$productId") {
            header(HttpHeaders.Authorization, token)
        }.body()

    // Post Management
    suspend fun getAdminPosts(
        token: String,
        search: String? = null,
        postType: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminPostResponse> =
        httpClient.get("api/admin/posts") {
            header(HttpHeaders.Authorization, token)
            search?.let { parameter("search", it) }
            postType?.let { parameter("post_type", it) }
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun deletePost(token: String, postUid: String): MessageResponse =
        httpClient.delete("api/admin/posts/$postUid") {
            header(HttpHeaders.Authorization, token)
        }.body()

    // Comment Management
    suspend fun getAdminComments(
        token: String,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminCommentResponse> =
        httpClient.get("api/admin/comments") {
            header(HttpHeaders.Authorization, token)
            search?.let { parameter("search", it) }
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun deleteComment(token: String, commentId: Int): MessageResponse =
        httpClient.delete("api/admin/comments/$commentId") {
            header(HttpHeaders.Authorization, token)
        }.body()

    // Follow Stats
    suspend fun getFollowStats(token: String): FollowStatsResponse =
        httpClient.get("api/admin/follows/stats") {
            header(HttpHeaders.Authorization, token)
        }.body()

    // Notification Management
    suspend fun getAdminNotifications(
        token: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminNotificationResponse> =
        httpClient.get("api/admin/notifications") {
            header(HttpHeaders.Authorization, token)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun sendNotification(
        token: String,
        request: SendNotificationRequest
    ): MessageResponse =
        httpClient.post("api/admin/notifications/send") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Category Management
    suspend fun getCategories(parentId: String? = null): List<AdminCategoryResponse> =
        httpClient.get("api/v1/marketplace/categories") {
            parentId?.let { parameter("parent_id", it) }
        }.body()

    suspend fun createCategory(
        token: String,
        request: CategoryCreateRequest
    ): AdminCategoryResponse =
        httpClient.post("api/v1/admin/marketplace/categories") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun updateCategory(
        token: String,
        categoryId: String,
        request: CategoryUpdateRequest
    ): AdminCategoryResponse =
        httpClient.put("api/v1/admin/marketplace/categories/$categoryId") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun deleteCategory(token: String, categoryId: String): MessageResponse =
        httpClient.delete("api/v1/admin/marketplace/categories/$categoryId") {
            header(HttpHeaders.Authorization, token)
        }.body()

    // Affiliate Sales Management (Admin)
    suspend fun getAdminAffiliateSales(
        token: String,
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminAffiliateSaleResponse> =
        httpClient.get("api/v1/admin/sales") {
            header(HttpHeaders.Authorization, token)
            status?.let { parameter("status", it) }
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun updateAffiliateSaleStatus(
        token: String,
        saleId: String,
        request: SaleStatusUpdateRequest
    ): AdminAffiliateSaleResponse =
        httpClient.patch("api/v1/admin/sales/$saleId/status") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
// Request models
@Serializable
data class AdminLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserActionRequest(
    val user_ids: List<String>
)

@Serializable
data class StatusUpdateRequest(
    val status: String
)

@Serializable
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

@Serializable
data class ProductUpdateResponse(
    val id: String,
    val name: String,
    val description: String?,
    @SerialName("main_image_url") val mainImageUrl: String?,
    val images: List<String>,
    @SerialName("original_price") val originalPrice: Double,
    @SerialName("selling_price") val sellingPrice: Double,
    @SerialName("commission_rate") val commissionRate: Double,
    @SerialName("category_name") val categoryName: String?,
    @SerialName("is_featured") val isFeatured: Boolean,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("total_sales") val totalSales: Int
)

// Response models
@Serializable
data class AdminLoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val admin: AdminInfo
)

@Serializable
data class AdminInfo(
    val id: Int,
    val username: String,
    val email: String,
    val role: String
)

@Serializable
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

@Serializable
data class RecentUserResponse(
    val id: String,
    val username: String,
    val email: String,
    val display_name: String,
    val is_verified: Boolean,
    val created_at: String
)

@Serializable
data class RecentOrderResponse(
    val id: Int,
    val buyer_email: String,
    val total_amount: Double,
    val status: String,
    val created_at: String
)

@Serializable
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

@Serializable
data class MessageResponse(
    val message: String,
    val count: Int? = null
)

// CJ Dropshipping Models
@Serializable
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

@Serializable
data class CJVariant(
    val variant_id: String,
    val variant_name: String,
    val variant_sku: String? = null,
    val sell_price: Double,
    val stock: Int? = null
)

@Serializable
data class CJSearchResponse(
    val products: List<CJProduct>,
    val total: Int,
    val page: Int,
    val page_size: Int
)

@Serializable
data class CJImportRequest(
    val cj_product_id: String,
    val cj_variant_id: String? = null,
    val commission_rate: Double = 10.0,
    val category_id: String? = null,
    val custom_description: String? = null,
    val selling_price: Double? = null
)

@Serializable
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
@Serializable
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
@Serializable
data class AdminCommentResponse(
    val id: Int,
    val username: String,
    val user_uid: String,
    val post_uid: String,
    val content: String,
    val created_at: String
)

// Admin Follow Stats Models
@Serializable
data class FollowStatsResponse(
    val total_follows: Int,
    val new_follows_today: Int,
    val new_follows_this_week: Int,
    val top_followed_users: List<TopFollowedUser>
)

@Serializable
data class TopFollowedUser(
    val uid: String,
    val username: String,
    val display_name: String,
    val followers_count: Int
)

// Admin Notification Models
@Serializable
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

@Serializable
data class SendNotificationRequest(
    val title: String,
    val body: String,
    val type: String = "admin_broadcast",
    val target_user_uids: List<String>? = null
)

// Category Management Models
@Serializable
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

@Serializable
data class CategoryCreateRequest(
    val name: String,
    val name_ar: String? = null,
    val slug: String,
    val icon_url: String? = null,
    val parent_id: String? = null,
    val display_order: Int = 0,
    val is_active: Boolean = true
)

@Serializable
data class CategoryUpdateRequest(
    val name: String? = null,
    val name_ar: String? = null,
    val icon_url: String? = null,
    val display_order: Int? = null,
    val is_active: Boolean? = null
)

// Affiliate Sales Admin Models
@Serializable
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

@Serializable
data class SaleStatusUpdateRequest(
    val status: String,
    val payment_reference: String? = null,
    val payment_notes: String? = null
)

