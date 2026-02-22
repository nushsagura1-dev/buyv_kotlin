package com.project.e_commerce.domain.usecase.cart

import com.project.e_commerce.domain.model.Cart
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CartRepository

/**
 * Use case pour récupérer le panier utilisateur.
 * 
 * Gère la récupération et la synchronisation du panier.
 */
class GetCartUseCase(
    private val cartRepository: CartRepository
) {
    /**
     * Récupère le panier de l'utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant le panier
     */
    suspend operator fun invoke(userId: String): Result<Cart> {
        if (userId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("User ID cannot be empty")
            )
        }
        
        return cartRepository.getUserCart(userId)
    }

    /**
     * Récupère le flux du panier pour les mises à jour en temps réel.
     */
    operator fun invoke(userId: String, observe: Boolean): kotlinx.coroutines.flow.Flow<Cart?> {
        // Validation basic
        if (userId.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return cartRepository.getUserCartFlow(userId)
    }
    
    /**
     * Synchronise le panier avec le serveur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant le panier synchronisé
     */
    suspend fun syncCart(userId: String): Result<Cart> {
        return cartRepository.syncCart(userId)
    }
}
