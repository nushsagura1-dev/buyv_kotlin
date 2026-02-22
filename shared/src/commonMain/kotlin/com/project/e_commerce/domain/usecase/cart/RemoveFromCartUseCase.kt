package com.project.e_commerce.domain.usecase.cart

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CartRepository

/**
 * Use case pour supprimer un article du panier.
 * 
 * Gère la validation et la suppression d'articles.
 */
class RemoveFromCartUseCase(
    private val cartRepository: CartRepository
) {
    /**
     * Supprime un article du panier.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param lineId Identifiant de l'article dans le panier
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend operator fun invoke(userId: String, lineId: String): Result<Unit> {
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
        
        return cartRepository.removeFromCart(userId, lineId)
    }
}
