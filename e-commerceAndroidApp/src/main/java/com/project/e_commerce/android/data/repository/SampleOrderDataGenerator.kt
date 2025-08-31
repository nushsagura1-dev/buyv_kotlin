package com.project.e_commerce.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.e_commerce.android.domain.model.Order
import com.project.e_commerce.android.domain.model.OrderItem
import com.project.e_commerce.android.domain.model.OrderStatus
import com.project.e_commerce.android.domain.model.Address
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.*

class SampleOrderDataGenerator(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "SampleOrderDataGenerator"
        private const val ORDERS_COLLECTION = "orders"
    }

    suspend fun generateSampleOrders() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "No authenticated user found")
            return
        }

        Log.d(TAG, "Generating sample orders for user: $userId")

        val sampleAddress = Address(
            id = "addr1",
            name = "John Doe",
            street = "123 Main Street",
            city = "New York",
            state = "NY",
            zipCode = "10001",
            country = "USA",
            phone = "+1-234-567-8900"
        )

        val sampleOrders = listOf(
            // Recent delivered order
            Order(
                userId = userId,
                orderNumber = "ORD${System.currentTimeMillis()}001",
                items = listOf(
                    OrderItem(
                        id = "item1",
                        productId = "product1",
                        productName = "Tom Ford Black Orchid",
                        productImage = "https://example.com/perfume1.jpg",
                        price = 89.99,
                        quantity = 1
                    ),
                    OrderItem(
                        id = "item2",
                        productId = "product2",
                        productName = "Designer Handbag",
                        productImage = "https://example.com/bag1.jpg",
                        price = 249.99,
                        quantity = 1
                    )
                ),
                status = OrderStatus.DELIVERED,
                subtotal = 339.98,
                shipping = 8.00,
                tax = 27.20,
                total = 375.18,
                shippingAddress = sampleAddress,
                paymentMethod = "Mastercard",
                createdAt = Timestamp(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)), // 7 days ago
                updatedAt = Timestamp(Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000))  // 2 days ago
            ),

            // Pending order
            Order(
                userId = userId,
                orderNumber = "ORD${System.currentTimeMillis()}002",
                items = listOf(
                    OrderItem(
                        id = "item3",
                        productId = "product3",
                        productName = "Wireless Headphones",
                        productImage = "https://example.com/headphones1.jpg",
                        price = 199.99,
                        quantity = 1
                    )
                ),
                status = OrderStatus.PENDING,
                subtotal = 199.99,
                shipping = 8.00,
                tax = 16.00,
                total = 223.99,
                shippingAddress = sampleAddress,
                paymentMethod = "PayPal",
                createdAt = Timestamp(Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000)), // 2 days ago
                updatedAt = Timestamp(Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000))
            ),

            // Shipped order
            Order(
                userId = userId,
                orderNumber = "ORD${System.currentTimeMillis()}003",
                items = listOf(
                    OrderItem(
                        id = "item4",
                        productId = "product4",
                        productName = "Cotton T-Shirt",
                        productImage = "https://example.com/tshirt1.jpg",
                        price = 29.99,
                        quantity = 2
                    ),
                    OrderItem(
                        id = "item5",
                        productId = "product5",
                        productName = "Blue Jeans",
                        productImage = "https://example.com/jeans1.jpg",
                        price = 79.99,
                        quantity = 1
                    )
                ),
                status = OrderStatus.SHIPPED,
                subtotal = 139.97,
                shipping = 8.00,
                tax = 11.20,
                total = 159.17,
                shippingAddress = sampleAddress,
                paymentMethod = "Mastercard",
                trackingNumber = "TRK123456789",
                createdAt = Timestamp(Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000)), // 5 days ago
                updatedAt = Timestamp(Date(System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000))  // 1 day ago
            ),

            // Cancelled order
            Order(
                userId = userId,
                orderNumber = "ORD${System.currentTimeMillis()}004",
                items = listOf(
                    OrderItem(
                        id = "item6",
                        productId = "product6",
                        productName = "Smart Watch",
                        productImage = "https://example.com/watch1.jpg",
                        price = 299.99,
                        quantity = 1
                    )
                ),
                status = OrderStatus.CANCELED,
                subtotal = 299.99,
                shipping = 8.00,
                tax = 24.00,
                total = 331.99,
                shippingAddress = sampleAddress,
                paymentMethod = "Mastercard",
                createdAt = Timestamp(Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000)), // 10 days ago
                updatedAt = Timestamp(Date(System.currentTimeMillis() - 9 * 24 * 60 * 60 * 1000))   // 9 days ago
            ),

            // Another delivered order (older)
            Order(
                userId = userId,
                orderNumber = "ORD${System.currentTimeMillis()}005",
                items = listOf(
                    OrderItem(
                        id = "item7",
                        productId = "product7",
                        productName = "Running Shoes",
                        productImage = "https://example.com/shoes1.jpg",
                        price = 129.99,
                        quantity = 1
                    )
                ),
                status = OrderStatus.DELIVERED,
                subtotal = 129.99,
                shipping = 8.00,
                tax = 10.40,
                total = 148.39,
                shippingAddress = sampleAddress,
                paymentMethod = "PayPal",
                createdAt = Timestamp(Date(System.currentTimeMillis() - 15 * 24 * 60 * 60 * 1000)), // 15 days ago
                updatedAt = Timestamp(Date(System.currentTimeMillis() - 12 * 24 * 60 * 60 * 1000))  // 12 days ago
            )
        )

        try {
            // Create all sample orders
            sampleOrders.forEach { order ->
                val orderId = firestore.collection(ORDERS_COLLECTION).document().id
                val orderWithId = order.copy(id = orderId)

                val orderData = orderToMap(orderWithId)

                firestore.collection(ORDERS_COLLECTION)
                    .document(orderId)
                    .set(orderData)
                    .await()

                Log.d(TAG, "Created sample order: ${order.orderNumber}")
            }

            Log.d(TAG, "Successfully generated ${sampleOrders.size} sample orders")

        } catch (e: Exception) {
            Log.e(TAG, "Error generating sample orders: ${e.message}", e)
        }
    }

    private fun orderToMap(order: Order): Map<String, Any> {
        return mapOf(
            "id" to order.id,
            "userId" to order.userId,
            "orderNumber" to order.orderNumber,
            "items" to order.items.map { orderItemToMap(it) },
            "status" to order.status.name,
            "subtotal" to order.subtotal,
            "shipping" to order.shipping,
            "tax" to order.tax,
            "total" to order.total,
            "shippingAddress" to (order.shippingAddress?.let { addressToMap(it) }
                ?: emptyMap<String, Any>()),
            "paymentMethod" to order.paymentMethod,
            "createdAt" to (order.createdAt ?: Timestamp.now()),
            "updatedAt" to (order.updatedAt ?: Timestamp.now()),
            "estimatedDelivery" to (order.estimatedDelivery ?: ""),
            "trackingNumber" to (order.trackingNumber ?: ""),
            "notes" to order.notes
        )
    }

    private fun orderItemToMap(item: OrderItem): Map<String, Any> {
        return mapOf(
            "id" to item.id,
            "productId" to item.productId,
            "productName" to item.productName,
            "productImage" to item.productImage,
            "price" to item.price,
            "quantity" to item.quantity,
            "size" to (item.size ?: ""),
            "color" to (item.color ?: ""),
            "attributes" to item.attributes
        )
    }

    private fun addressToMap(address: Address): Map<String, Any> {
        return mapOf(
            "id" to address.id,
            "name" to address.name,
            "street" to address.street,
            "city" to address.city,
            "state" to address.state,
            "zipCode" to address.zipCode,
            "country" to address.country,
            "phone" to address.phone,
            "isDefault" to address.isDefault
        )
    }
}