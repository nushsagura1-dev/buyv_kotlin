package com.project.e_commerce.android.data.model.admin

import com.google.gson.annotations.SerializedName

// Authentication
data class AdminLoginRequest(
    val username: String,
    val password: String
)

data class AdminLoginResponse(
    val success: Boolean,
    val message: String,
    val admin: Admin?,
    val token: String?
)

data class Admin(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

// Common Response
data class MessageResponse(
    val message: String,
    val success: Boolean = true
)

// Stats
data class DashboardStats(
    @SerializedName("total_users") val totalUsers: Int,
    @SerializedName("total_orders") val totalOrders: Int,
    @SerializedName("total_revenue") val totalRevenue: Double,
    @SerializedName("total_products") val totalProducts: Int,
    @SerializedName("pending_withdrawals") val pendingWithdrawals: Int,
    @SerializedName("active_commissions") val activeCommissions: Int
)

// User Management
data class UserVerificationRequest(
    @SerializedName("is_verified") val isVerified: Boolean
)

// Admin-specific Product
data class AdminProduct(
    val id: String,
    val name: String,
    val description: String?,
    @SerializedName("main_image_url") val mainImage: String?,
    val images: List<String>?,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("commission_rate") val commissionRate: Double,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("is_featured") val isFeatured: Boolean,
    val status: String,
    @SerializedName("total_sales") val totalSales: Int,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("original_price") val originalPrice: Double?
)

// CJ Dropshipping
data class CJSearchResponse(
    val products: List<CJProduct>,
    val total: Int,
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class CJProduct(
    @SerializedName("cj_product_id") val cjProductId: String,
    val name: String,
    val description: String?,
    @SerializedName("main_image") val mainImage: String?,
    val images: List<String>,
    val price: Double,
    val category: String?,
    @SerializedName("in_stock") val inStock: Boolean
)

data class CJImportRequest(
    @SerializedName("cj_product_id") val cj_product_id: String,
    @SerializedName("cj_variant_id") val cj_variant_id: String? = null,
    @SerializedName("commission_rate") val commission_rate: Double,
    @SerializedName("category_id") val category_id: String? = null,
    @SerializedName("custom_description") val custom_description: String? = null
)

data class CJImportResponse(
    val id: String,
    val name: String,
    @SerializedName("main_image_url") val mainImageUrl: String?,
    @SerializedName("selling_price") val sellingPrice: Double,
    @SerializedName("original_price") val originalPrice: Double,
    @SerializedName("commission_rate") val commissionRate: Double,
    @SerializedName("cj_product_id") val cjProductId: String?,
    val status: String,
    val message: String? = null
)

data class AdminProductsResponse(
    @SerializedName("items") val products: List<AdminProduct>,
    val total: Int,
    val page: Int,
    val limit: Int,
    @SerializedName("total_pages") val totalPages: Int
)
