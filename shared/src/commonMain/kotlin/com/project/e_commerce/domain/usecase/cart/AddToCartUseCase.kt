package com.project.e_commerce.domain.usecase.cart

import com.project.e_commerce.domain.model.CartItem
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CartRepository

/**
 * Use case pour ajouter un article au panier.
 * 
 * Gère la validation et l'ajout d'articles au panier utilisateur.
 */
class AddToCartUseCase(
    private val cartRepository: CartRepository
) {
    /**
     * Ajoute un article au panier.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param item Article à ajouter
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend operator fun invoke(userId: String, item: CartItem): Result<Unit> {
        // Validation
        if (userId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("User ID cannot be empty")
            )
        }
        
        if (item.productId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("Product ID cannot be empty")
            )
        }
        
        if (item.quantity <= 0) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("Quantity must be greater than 0")
            )
        }
        
        if (item.price < 0) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("Price cannot be negative")
            )
        }
        
        return cartRepository.addToCart(userId, item)
    }
}
