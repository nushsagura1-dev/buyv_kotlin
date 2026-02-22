package com.project.e_commerce.domain.usecase.cart

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CartRepository

/**
 * Use case pour mettre à jour la quantité d'un article du panier.
 * 
 * Gère la validation et la mise à jour de quantité.
 */
class UpdateCartItemUseCase(
    private val cartRepository: CartRepository
) {
    /**
     * Met à jour la quantité d'un article.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param lineId Identifiant de l'article dans le panier
     * @param quantity Nouvelle quantité
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend operator fun invoke(userId: String, lineId: String, quantity: Int): Result<Unit> {
        // Validation
        if (userId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("User ID cannot be empty")
            )
        }
        
        if (lineId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("Line ID cannot be empty")
            )
        }
        
        if (quantity < 0) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("Quantity cannot be negative")
            )
        }
        
        return cartRepository.updateQuantity(userId, lineId, quantity)
    }
}
