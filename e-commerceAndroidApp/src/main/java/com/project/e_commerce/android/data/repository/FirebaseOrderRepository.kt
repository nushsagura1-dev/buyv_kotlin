package com.project.e_commerce.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.project.e_commerce.android.domain.repository.OrderRepository
import com.project.e_commerce.android.domain.model.Order
import com.project.e_commerce.android.domain.model.OrderItem
import com.project.e_commerce.android.domain.model.OrderStatus
import com.project.e_commerce.android.domain.model.Address
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseOrderRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : OrderRepository {

    companion object {
        private const val ORDERS_COLLECTION = "orders"
        private const val USERS_COLLECTION = "users"
        private const val TAG = "FirebaseOrderRepository"
    }

    init {
        Log.d(TAG, "FirebaseOrderRepository initialized")
    }

    override suspend fun createOrder(order: Order): Result<String> = runCatching {
        Log.d(TAG, "Creating new order for user: ${order.userId}")

        // Generate order ID and number if not provided
        val orderId = order.id.ifEmpty { firestore.collection(ORDERS_COLLECTION).document().id }
        val orderNumber = order.orderNumber.ifEmpty { generateOrderNumber() }

        val orderWithIds = order.copy(
            id = orderId,
            orderNumber = orderNumber,
            createdAt = order.createdAt ?: com.google.firebase.Timestamp.now(),
            updatedAt = com.google.firebase.Timestamp.now()
        )

        // Convert order to map for Firestore
        val orderData = orderToMap(orderWithIds)

        // Create order document
        firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .set(orderData)
            .await()

        Log.d(TAG, "Successfully created order: $orderNumber")
        orderId
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> =
        runCatching {
            Log.d(TAG, "Updating order status: $orderId to ${status.displayName}")

            val updateData = mapOf(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(ORDERS_COLLECTION)
                .document(orderId)
                .update(updateData)
                .await()

            Log.d(TAG, "Successfully updated order status")
        }

    override fun getUserOrders(userId: String): Flow<List<Order>> = callbackFlow {
        Log.d(TAG, "Getting orders for user: $userId")

        val query = firestore.collection(ORDERS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error getting user orders: ${error.message}")
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val orders = snapshot.documents.mapNotNull { doc ->
                    try {
                        mapToOrder(doc.data ?: emptyMap(), doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing order document ${doc.id}: ${e.message}")
                        null
                    }
                }

                trySend(orders)
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun getOrderById(orderId: String): Result<Order> = runCatching {
        Log.d(TAG, "Getting order by ID: $orderId")

        val document = firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .get()
            .await()

        if (!document.exists()) {
            throw NoSuchElementException("Order not found: $orderId")
        }

        mapToOrder(document.data ?: emptyMap(), document.id)
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> = runCatching {
        Log.d(TAG, "Canceling order: $orderId")

        // Check if order can be canceled (not shipped/delivered)
        val order = getOrderById(orderId).getOrThrow()

        if (order.status in listOf(
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELED
            )
        ) {
            throw IllegalStateException("Cannot cancel order with status: ${order.status.displayName}")
        }

        updateOrderStatus(orderId, OrderStatus.CANCELED).getOrThrow()

        Log.d(TAG, "Successfully canceled order")
    }

    override fun getUserOrdersByStatus(userId: String, status: OrderStatus): Flow<List<Order>> =
        callbackFlow {
            Log.d(TAG, "Getting orders for user: $userId with status: ${status.displayName}")

            val query = firestore.collection(ORDERS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", status.name)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting user orders by status: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            mapToOrder(doc.data ?: emptyMap(), doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing order document ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    trySend(orders)
                }
            }

            awaitClose { listener.remove() }
        }

    override suspend fun updateTrackingNumber(
        orderId: String,
        trackingNumber: String
    ): Result<Unit> = runCatching {
        Log.d(TAG, "Updating tracking number for order: $orderId")

        val updateData = mapOf(
            "trackingNumber" to trackingNumber,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .update(updateData)
            .await()

        Log.d(TAG, "Successfully updated tracking number")
    }

    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "ORD$timestamp$random"
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
            "createdAt" to (order.createdAt ?: com.google.firebase.Timestamp.now()),
            "updatedAt" to (order.updatedAt ?: com.google.firebase.Timestamp.now()),
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

    @Suppress("UNCHECKED_CAST")
    private fun mapToOrder(data: Map<String, Any>, documentId: String): Order {
        return Order(
            id = data["id"] as? String ?: documentId,
            userId = data["userId"] as? String ?: "",
            orderNumber = data["orderNumber"] as? String ?: "",
            items = (data["items"] as? List<Map<String, Any>>)?.map { mapToOrderItem(it) }
                ?: emptyList(),
            status = OrderStatus.fromString(data["status"] as? String ?: "PENDING"),
            subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            shipping = (data["shipping"] as? Number)?.toDouble() ?: 0.0,
            tax = (data["tax"] as? Number)?.toDouble() ?: 0.0,
            total = (data["total"] as? Number)?.toDouble() ?: 0.0,
            shippingAddress = (data["shippingAddress"] as? Map<String, Any>)?.let { mapToAddress(it) },
            paymentMethod = data["paymentMethod"] as? String ?: "",
            createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
            updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp,
            estimatedDelivery = data["estimatedDelivery"] as? com.google.firebase.Timestamp,
            trackingNumber = data["trackingNumber"] as? String,
            notes = data["notes"] as? String ?: ""
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToOrderItem(data: Map<String, Any>): OrderItem {
        return OrderItem(
            id = data["id"] as? String ?: "",
            productId = data["productId"] as? String ?: "",
            productName = data["productName"] as? String ?: "",
            productImage = data["productImage"] as? String ?: "",
            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
            quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
            size = data["size"] as? String,
            color = data["color"] as? String,
            attributes = (data["attributes"] as? Map<String, String>) ?: emptyMap()
        )
    }

    private fun mapToAddress(data: Map<String, Any>): Address {
        return Address(
            id = data["id"] as? String ?: "",
            name = data["name"] as? String ?: "",
            street = data["street"] as? String ?: "",
            city = data["city"] as? String ?: "",
            state = data["state"] as? String ?: "",
            zipCode = data["zipCode"] as? String ?: "",
            country = data["country"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            isDefault = data["isDefault"] as? Boolean ?: false
        )
    }
}