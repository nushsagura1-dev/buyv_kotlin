package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.OrderRepository
import com.project.e_commerce.android.domain.model.Order
import com.project.e_commerce.android.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

class GetUserOrdersUseCase(
    private val orderRepository: OrderRepository
) {
    operator fun invoke(userId: String): Flow<List<Order>> {
        return orderRepository.getUserOrders(userId)
    }

    fun byStatus(userId: String, status: OrderStatus): Flow<List<Order>> {
        return orderRepository.getUserOrdersByStatus(userId, status)
    }
}