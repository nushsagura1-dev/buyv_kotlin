package com.project.e_commerce.domain.usecase.order

import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.OrderRepository

/**
 * Use Case pour récupérer les détails d'une commande spécifique.
 * 
 * @property orderRepository Repository des commandes
 */
class GetOrderDetailsUseCase(
    private val orderRepository: OrderRepository
) {
    /**
     * Récupère les détails d'une commande.
     * 
     * @param orderId Identifiant de la commande
     * @return Result contenant la commande avec tous ses détails
     */
    suspend operator fun invoke(orderId: String): Result<Order> {
        // Validation de l'ID
        if (orderId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError(
                    "Order ID cannot be empty"
                )
            )
        }
        
        return orderRepository.getOrderById(orderId)
    }
}
