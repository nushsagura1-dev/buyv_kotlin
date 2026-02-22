package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.OrderRepository
import com.project.e_commerce.domain.model.OrderStatus

class UpdateOrderStatusUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String, status: OrderStatus): Result<Unit> {
        return orderRepository.updateOrderStatus(orderId, status)
    }
}