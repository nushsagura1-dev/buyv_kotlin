package com.project.e_commerce.android.domain.repository

import com.project.e_commerce.android.domain.model.Order
import com.project.e_commerce.android.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OrderRepository {

    /**
     * Create a new order
     */
    suspend fun createOrder(order: Order): Result<String>

    /**
     * Update order status
     */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>

    /**
     * Get all orders for a specific user
     */
    fun getUserOrders(userId: String): Flow<List<Order>>

    /**
     * Get a specific order by ID
     */
    suspend fun getOrderById(orderId: String): Result<Order>

    /**
     * Cancel an order
     */
    suspend fun cancelOrder(orderId: String): Result<Unit>

    /**
     * Get orders filtered by status
     */
    fun getUserOrdersByStatus(userId: String, status: OrderStatus): Flow<List<Order>>

    /**
     * Update order tracking number
     */
    suspend fun updateTrackingNumber(orderId: String, trackingNumber: String): Result<Unit>
}