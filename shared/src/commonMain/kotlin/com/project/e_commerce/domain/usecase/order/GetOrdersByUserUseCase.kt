package com.project.e_commerce.domain.usecase.order

import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.OrderRepository

/**
 * Use Case pour récupérer toutes les commandes d'un utilisateur.
 * 
 * @property orderRepository Repository des commandes
 */
class GetOrdersByUserUseCase(
    private val orderRepository: OrderRepository
) {
    /**
     * Récupère toutes les commandes d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste des commandes (triées par date décroissante)
     */
    suspend operator fun invoke(userId: String): Result<List<Order>> {
        // Validation de l'ID utilisateur
        if (userId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError(
                    "User ID cannot be empty"
                )
            )
        }
        
        return orderRepository.getOrdersByUser(userId)
    }
}
