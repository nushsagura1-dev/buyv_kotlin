package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.OrderRepository
import com.project.e_commerce.android.domain.model.Order

class CreateOrderUseCase(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(order: Order): Result<String> {
        return orderRepository.createOrder(order)
    }
}