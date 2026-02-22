package com.project.e_commerce.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * Order model for admin management
 */
data class Order(
    val id: String,
    @SerializedName("order_number") val orderNumber: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_email") val userEmail: String?,
    val items: List<OrderItem>,
    @SerializedName("total_amount") val totalAmount: Double,
    val status: String, // pending, processing, shipped, delivered, cancelled
    @SerializedName("payment_status") val paymentStatus: String,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("shipping_address") val shippingAddress: ShippingAddress?,
    @SerializedName("tracking_number") val trackingNumber: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class OrderItem(
    val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("product_image") val productImage: String?,
    val quantity: Int,
    val price: Double,
    val total: Double
)

data class ShippingAddress(
    val name: String,
    val phone: String,
    val address: String,
    val city: String,
    val state: String?,
    val country: String,
    @SerializedName("postal_code") val postalCode: String
)

data class UpdateOrderStatusRequest(
    val status: String,
    @SerializedName("tracking_number") val trackingNumber: String? = null,
    val notes: String? = null
)
