package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.OrderRepository
import com.project.e_commerce.domain.model.Order

class CreateOrderUseCase(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(order: Order): Result<String> {
        return repository.createOrder(order)
    }
}
