package com.project.e_commerce.domain.usecase.order

import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.OrderRepository

/**
 * Use Case pour créer une nouvelle commande.
 * 
 * Valide la commande et appelle le repository pour l'enregistrer.
 * 
 * @property orderRepository Repository des commandes
 */
class CreateOrderUseCase(
    private val orderRepository: OrderRepository
) {
    /**
     * Crée une nouvelle commande.
     * 
     * @param order Commande à créer (doit contenir items, shipping address, etc.)
     * @return Result contenant la commande créée avec son ID généré
     */
    suspend operator fun invoke(order: Order): Result<Order> {
        // Validation de base
        if (order.items.isEmpty()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError(
                    "Order must contain at least one item"
                )
            )
        }
        
        if (order.total <= 0.0) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError(
                    "Order total must be greater than zero"
                )
            )
        }
        
        // Délégation au repository
        return orderRepository.createOrder(order)
    }
}
