package com.project.e_commerce.domain.usecase.order

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.OrderRepository

/**
 * Use Case pour annuler une commande.
 * 
 * Vérifie si la commande peut être annulée avant de procéder.
 * 
 * @property orderRepository Repository des commandes
 */
class CancelOrderUseCase(
    private val orderRepository: OrderRepository
) {
    /**
     * Annule une commande si elle est annulable.
     * 
     * @param orderId Identifiant de la commande
     * @param reason Raison de l'annulation
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend operator fun invoke(orderId: String, reason: String): Result<Unit> {
        // Validation
        if (orderId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError(
                    "Order ID cannot be empty"
                )
            )
        }
        
        if (reason.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError(
                    "Cancellation reason is required"
                )
            )
        }
        
        // Vérifier si la commande peut être annulée
        return when (val canCancelResult = orderRepository.canCancelOrder(orderId)) {
            is Result.Success -> {
                if (canCancelResult.data) {
                    orderRepository.cancelOrder(orderId, reason)
                } else {
                    Result.Error(
                        com.project.e_commerce.domain.model.ApiError.ValidationError(
                            "This order cannot be cancelled (already shipped or delivered)"
                        )
                    )
                }
            }
            is Result.Error -> canCancelResult
            is Result.Loading -> Result.Loading
        }
    }
}
