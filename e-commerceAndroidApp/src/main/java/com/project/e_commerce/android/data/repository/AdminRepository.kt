package com.project.e_commerce.android.data.repository

import com.project.e_commerce.android.data.api.*
import com.project.e_commerce.android.data.model.admin.AdminProduct
import javax.inject.Inject

class AdminRepository @Inject constructor(
    private val adminApi: AdminApi
) {
    
    suspend fun adminLogin(email: String, password: String): AdminLoginResponse {
        return adminApi.adminLogin(AdminLoginRequest(email, password))
    }
    
    suspend fun getDashboardStats(token: String): DashboardStatsResponse {
        return adminApi.getDashboardStats("Bearer $token")
    }
    
    suspend fun getRecentUsers(token: String, limit: Int = 10): List<RecentUserResponse> {
        return adminApi.getRecentUsers("Bearer $token", limit)
    }
    
    suspend fun getRecentOrders(token: String, limit: Int = 10): List<RecentOrderResponse> {
        return adminApi.getRecentOrders("Bearer $token", limit)
    }
    
    suspend fun getUsers(
        token: String,
        search: String? = null,
        isVerified: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<UserManagementResponse> {
        return adminApi.getUsers("Bearer $token", search, isVerified, limit, offset)
    }
    
    suspend fun verifyUsers(token: String, userIds: List<String>): MessageResponse {
        return adminApi.verifyUsers("Bearer $token", UserActionRequest(userIds))
    }
    
    suspend fun unverifyUsers(token: String, userIds: List<String>): MessageResponse {
        return adminApi.unverifyUsers("Bearer $token", UserActionRequest(userIds))
    }
    
    suspend fun deleteUser(token: String, userUid: String): MessageResponse {
        return adminApi.deleteUser("Bearer $token", userUid)
    }
    
    // Individual user actions (convenience methods)
    suspend fun verifyUser(userId: String) {
        // TODO: Get token from secure storage
        verifyUsers("", listOf(userId))
    }
    
    suspend fun unverifyUser(userId: String) {
        // TODO: Get token from secure storage
        unverifyUsers("", listOf(userId))
    }
    
    suspend fun getUsers(): List<com.project.e_commerce.android.data.model.User> {
        // TODO: Get token from secure storage
        val userResponses = getUsers("", null, null, 100, 0)
        return userResponses.map { response ->
            com.project.e_commerce.android.data.model.User(
                id = response.id,
                email = response.email,
                username = response.username,
                displayName = response.display_name,
                profileImageUrl = null, // UserManagementResponse doesn't have photo_url
                isVerified = response.is_verified,
                isActive = true,
                bio = null,
                followersCount = response.followers_count,
                followingCount = response.following_count,
                postsCount = response.reels_count,
                createdAt = response.created_at,
                updatedAt = null
            )
        }
    }
    
    // Order Management
    suspend fun getAllOrders(token: String): List<com.project.e_commerce.android.data.model.Order> {
        return adminApi.getAllOrders("Bearer $token")
    }
    
    suspend fun getOrdersByStatus(token: String, status: String): List<com.project.e_commerce.android.data.model.Order> {
        return adminApi.getOrdersByStatus("Bearer $token", status)
    }
    
    suspend fun updateOrderStatus(token: String, orderId: Int, newStatus: String): MessageResponse {
        return adminApi.updateOrderStatus("Bearer $token", orderId, StatusUpdateRequest(newStatus))
    }
    
    // Commission Management
    suspend fun getAllCommissions(token: String): List<com.project.e_commerce.domain.model.Commission> {
        return adminApi.getAllCommissions("Bearer $token")
    }
    
    suspend fun getCommissionsByStatus(token: String, status: String): List<com.project.e_commerce.domain.model.Commission> {
        return adminApi.getCommissionsByStatus("Bearer $token", status)
    }
    
    suspend fun updateCommissionStatus(token: String, commissionId: Int, newStatus: String): MessageResponse {
        return adminApi.updateCommissionStatus("Bearer $token", commissionId, StatusUpdateRequest(newStatus))
    }
    
    // CJ Dropshipping Import
    suspend fun searchCJProducts(
        token: String,
        query: String,
        category: String? = null,
        page: Int = 1
    ): CJSearchResponse {
        return adminApi.searchCJProducts("Bearer $token", query, category, page)
    }
    
    suspend fun importCJProduct(
        token: String,
        cjProductId: String,
        cjVariantId: String? = null,
        commissionRate: Double = 10.0,
        categoryId: String? = null,
        customDescription: String? = null,
        sellingPrice: Double? = null
    ): CJImportResponse {
        return adminApi.importCJProduct(
            "Bearer $token",
            CJImportRequest(
                cj_product_id = cjProductId,
                cj_variant_id = cjVariantId,
                commission_rate = commissionRate,
                category_id = categoryId,
                custom_description = customDescription,
                selling_price = sellingPrice
            )
        )
    }
    
    suspend fun syncCJProduct(token: String, productId: String): MessageResponse {
        return adminApi.syncCJProduct("Bearer $token", productId)
    }
    
    suspend fun getProducts(page: Int = 1, limit: Int = 100): com.project.e_commerce.android.data.model.admin.AdminProductsResponse {
        return adminApi.getProducts(page = page, limit = limit, sortBy = null)
    }
    
    suspend fun updateProduct(token: String, productId: String, request: ProductUpdateRequest): ProductUpdateResponse {
        return adminApi.updateProduct("Bearer $token", productId, request)
    }
    
    suspend fun deleteProduct(token: String, productId: String): MessageResponse {
        return adminApi.deleteProduct("Bearer $token", productId)
    }

    // Post Management
    suspend fun getAdminPosts(
        token: String,
        search: String? = null,
        postType: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminPostResponse> {
        return adminApi.getAdminPosts("Bearer $token", search, postType, limit, offset)
    }

    suspend fun deletePost(token: String, postUid: String): MessageResponse {
        return adminApi.deletePost("Bearer $token", postUid)
    }

    // Comment Management
    suspend fun getAdminComments(
        token: String,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminCommentResponse> {
        return adminApi.getAdminComments("Bearer $token", search, limit, offset)
    }

    suspend fun deleteComment(token: String, commentId: Int): MessageResponse {
        return adminApi.deleteComment("Bearer $token", commentId)
    }

    // Follow Stats
    suspend fun getFollowStats(token: String): FollowStatsResponse {
        return adminApi.getFollowStats("Bearer $token")
    }

    // Notification Management
    suspend fun getAdminNotifications(
        token: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminNotificationResponse> {
        return adminApi.getAdminNotifications("Bearer $token", limit, offset)
    }

    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        type: String = "admin_broadcast",
        targetUserUids: List<String>? = null
    ): MessageResponse {
        return adminApi.sendNotification(
            "Bearer $token",
            SendNotificationRequest(
                title = title,
                body = body,
                type = type,
                target_user_uids = targetUserUids
            )
        )
    }

    // Category Management
    suspend fun getCategories(parentId: String? = null): List<AdminCategoryResponse> {
        return adminApi.getCategories(parentId)
    }

    suspend fun createCategory(token: String, request: CategoryCreateRequest): AdminCategoryResponse {
        return adminApi.createCategory("Bearer $token", request)
    }

    suspend fun updateCategory(token: String, categoryId: String, request: CategoryUpdateRequest): AdminCategoryResponse {
        return adminApi.updateCategory("Bearer $token", categoryId, request)
    }

    suspend fun deleteCategory(token: String, categoryId: String): MessageResponse {
        return adminApi.deleteCategory("Bearer $token", categoryId)
    }

    // Affiliate Sales Admin
    suspend fun getAdminAffiliateSales(
        token: String,
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<AdminAffiliateSaleResponse> {
        return adminApi.getAdminAffiliateSales("Bearer $token", status, limit, offset)
    }

    suspend fun updateAffiliateSaleStatus(
        token: String,
        saleId: String,
        request: SaleStatusUpdateRequest
    ): AdminAffiliateSaleResponse {
        return adminApi.updateAffiliateSaleStatus("Bearer $token", saleId, request)
    }
}
