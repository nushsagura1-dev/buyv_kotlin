package com.project.e_commerce.android.domain.repository

import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun createOrder(order: Order): Result<String>
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>
    fun getUserOrders(userId: String): Flow<List<Order>>
    suspend fun getOrderById(orderId: String): Result<Order>
    suspend fun cancelOrder(orderId: String): Result<Unit>
    fun getUserOrdersByStatus(userId: String, status: OrderStatus): Flow<List<Order>>
    suspend fun updateTrackingNumber(orderId: String, trackingNumber: String): Result<Unit>
}
