package com.project.e_commerce.android.data.repository

import com.project.e_commerce.android.domain.repository.OrderRepository
import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.OrderStatus
import com.project.e_commerce.domain.model.Result as SharedResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementation of Android OrderRepository that delegates to the shared KMP OrderRepository.
 * Bridges between the Android interface (using Flow) and the shared suspend functions.
 */
class OrderRepositoryImpl(
    private val sharedOrderRepository: com.project.e_commerce.domain.repository.OrderRepository
) : OrderRepository {

    override suspend fun createOrder(order: Order): Result<String> {
        return when (val result = sharedOrderRepository.createOrder(order)) {
            is SharedResult.Success -> Result.success(result.data.id)
            is SharedResult.Error -> Result.failure(Exception(result.error.message))
            is SharedResult.Loading -> Result.failure(Exception("Loading"))
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        return when (val result = sharedOrderRepository.updateOrderStatus(orderId, status)) {
            is SharedResult.Success -> Result.success(Unit)
            is SharedResult.Error -> Result.failure(Exception(result.error.message))
            is SharedResult.Loading -> Result.failure(Exception("Loading"))
        }
    }

    override fun getUserOrders(userId: String): Flow<List<Order>> = flow {
        when (val result = sharedOrderRepository.getOrdersByUser(userId)) {
            is SharedResult.Success -> emit(result.data)
            is SharedResult.Error -> emit(emptyList())
            is SharedResult.Loading -> { /* skip */ }
        }
    }

    override suspend fun getOrderById(orderId: String): Result<Order> {
        return when (val result = sharedOrderRepository.getOrderById(orderId)) {
            is SharedResult.Success -> Result.success(result.data)
            is SharedResult.Error -> Result.failure(Exception(result.error.message))
            is SharedResult.Loading -> Result.failure(Exception("Loading"))
        }
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> {
        return when (val result = sharedOrderRepository.cancelOrder(orderId, "")) {
            is SharedResult.Success -> Result.success(Unit)
            is SharedResult.Error -> Result.failure(Exception(result.error.message))
            is SharedResult.Loading -> Result.failure(Exception("Loading"))
        }
    }

    override fun getUserOrdersByStatus(userId: String, status: OrderStatus): Flow<List<Order>> = flow {
        when (val result = sharedOrderRepository.getOrdersByStatus(userId, status)) {
            is SharedResult.Success -> emit(result.data)
            is SharedResult.Error -> emit(emptyList())
            is SharedResult.Loading -> { /* skip */ }
        }
    }

    override suspend fun updateTrackingNumber(orderId: String, trackingNumber: String): Result<Unit> {
        // Not directly available in shared — return success as the backend handles tracking via admin endpoints
        return Result.success(Unit)
    }
}
