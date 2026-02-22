package com.project.e_commerce.domain.usecase.order

import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.OrderRepository

/**
 * Use Case pour récupérer les commandes récentes d'un utilisateur.
 * 
 * Utile pour afficher un aperçu des dernières commandes sur l'écran principal.
 * 
 * @property orderRepository Repository des commandes
 */
class GetRecentOrdersUseCase(
    private val orderRepository: OrderRepository
) {
    /**
     * Récupère les commandes récentes d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param limit Nombre maximum de commandes à retourner (par défaut 5)
     * @return Result contenant la liste des commandes récentes
     */
    suspend operator fun invoke(userId: String, limit: Int = 5): Result<List<Order>> {
        // Validation
        if (userId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError(
                    "User ID cannot be empty"
                )
            )
        }
        
        if (limit <= 0) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError(
                    "Limit must be greater than zero"
                )
            )
        }
        
        return orderRepository.getRecentOrders(userId, limit)
    }
}
