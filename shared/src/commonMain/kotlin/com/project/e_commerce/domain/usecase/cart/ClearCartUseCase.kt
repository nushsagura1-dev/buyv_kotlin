package com.project.e_commerce.domain.usecase.cart

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CartRepository

/**
 * Use case pour vider complètement le panier.
 * 
 * Supprime tous les articles du panier utilisateur.
 */
class ClearCartUseCase(
    private val cartRepository: CartRepository
) {
    /**
     * Vide le panier de l'utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend operator fun invoke(userId: String): Result<Unit> {
        if (userId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("User ID cannot be empty")
            )
        }
        
        return cartRepository.clearCart(userId)
    }
}
