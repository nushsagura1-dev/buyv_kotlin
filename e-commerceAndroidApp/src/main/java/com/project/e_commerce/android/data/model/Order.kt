package com.project.e_commerce.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Order model for admin management
 */
@Serializable
data class Order(
    val id: String,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String?,
    @SerialName("user_email") val userEmail: String?,
    val items: List<OrderItem>,
    @SerialName("total_amount") val totalAmount: Double,
    val status: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("payment_method") val paymentMethod: String?,
    @SerialName("shipping_address") val shippingAddress: ShippingAddress?,
    @SerialName("tracking_number") val trackingNumber: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class OrderItem(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("product_image") val productImage: String?,
    val quantity: Int,
    val price: Double,
    val total: Double
)

@Serializable
data class ShippingAddress(
    val name: String,
    val phone: String,
    val address: String,
    val city: String,
    val state: String?,
    val country: String,
    @SerialName("postal_code") val postalCode: String
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: String,
    @SerialName("tracking_number") val trackingNumber: String? = null,
    val notes: String? = null
)
