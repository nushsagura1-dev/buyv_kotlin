package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.OrderRepository

class CancelOrderUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String): Result<Unit> {
        return orderRepository.cancelOrder(orderId)
    }
}