package com.project.e_commerce.android.domain.model

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val subtotal: Double = 0.0,
    val shipping: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val shippingAddress: Address? = null,
    val paymentMethod: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val estimatedDelivery: Timestamp? = null,
    val trackingNumber: String? = null,
    val notes: String = ""
)

data class OrderItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val size: String? = null,
    val color: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

enum class OrderStatus(val displayName: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    CANCELED("Canceled"),
    RETURNED("Returned"),
    REFUNDED("Refunded");

    companion object {
        fun fromString(status: String): OrderStatus {
            return values().find { it.name.equals(status, ignoreCase = true) } ?: PENDING
        }
    }
}

data class Address(
    val id: String = "",
    val name: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "",
    val phone: String = "",
    val isDefault: Boolean = false
)