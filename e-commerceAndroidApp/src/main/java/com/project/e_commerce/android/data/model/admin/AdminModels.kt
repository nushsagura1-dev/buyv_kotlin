package com.project.e_commerce.android.data.model.admin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Authentication
@Serializable
data class AdminLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AdminLoginResponse(
    val success: Boolean,
    val message: String,
    val admin: Admin?,
    val token: String?
)

@Serializable
data class Admin(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

// Common Response
@Serializable
data class MessageResponse(
    val message: String,
    val success: Boolean = true
)

// Stats
@Serializable
data class DashboardStats(
    @SerialName("total_users") val totalUsers: Int,
    @SerialName("total_orders") val totalOrders: Int,
    @SerialName("total_revenue") val totalRevenue: Double,
    @SerialName("total_products") val totalProducts: Int,
    @SerialName("pending_withdrawals") val pendingWithdrawals: Int,
    @SerialName("active_commissions") val activeCommissions: Int
)

// User Management
@Serializable
data class UserVerificationRequest(
    @SerialName("is_verified") val isVerified: Boolean
)

// Admin-specific Product
@Serializable
data class AdminProduct(
    val id: String,
    val name: String,
    val description: String?,
    @SerialName("main_image_url") val mainImage: String?,
    val images: List<String>?,
    @SerialName("selling_price") val sellingPrice: Double,
    @SerialName("commission_rate") val commissionRate: Double,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("is_featured") val isFeatured: Boolean,
    val status: String,
    @SerialName("total_sales") val totalSales: Int,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("original_price") val originalPrice: Double?
)

// CJ Dropshipping
@Serializable
data class CJSearchResponse(
    val products: List<CJProduct>,
    val total: Int,
    val page: Int,
    @SerialName("total_pages") val totalPages: Int
)

@Serializable
data class CJProduct(
    @SerialName("cj_product_id") val cjProductId: String,
    val name: String,
    val description: String?,
    @SerialName("main_image") val mainImage: String?,
    val images: List<String>,
    val price: Double,
    val category: String?,
    @SerialName("in_stock") val inStock: Boolean
)

@Serializable
data class CJImportRequest(
    @SerialName("cj_product_id") val cj_product_id: String,
    @SerialName("cj_variant_id") val cj_variant_id: String? = null,
    @SerialName("commission_rate") val commission_rate: Double,
    @SerialName("category_id") val category_id: String? = null,
    @SerialName("custom_description") val custom_description: String? = null
)

@Serializable
data class CJImportResponse(
    val id: String,
    val name: String,
    @SerialName("main_image_url") val mainImageUrl: String?,
    @SerialName("selling_price") val sellingPrice: Double,
    @SerialName("original_price") val originalPrice: Double,
    @SerialName("commission_rate") val commissionRate: Double,
    @SerialName("cj_product_id") val cjProductId: String?,
    val status: String,
    val message: String? = null
)

@Serializable
data class AdminProductsResponse(
    @SerialName("items") val products: List<AdminProduct>,
    val total: Int,
    val page: Int,
    val limit: Int,
    @SerialName("total_pages") val totalPages: Int
)
