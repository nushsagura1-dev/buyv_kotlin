package com.project.e_commerce.domain.usecase.product

import com.project.e_commerce.domain.model.Product
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.ProductRepository

/**
 * Use case pour récupérer les détails d'un produit spécifique.
 * 
 * Gère la récupération et la validation d'un produit individuel.
 */
class GetProductDetailsUseCase(
    private val productRepository: ProductRepository
) {
    /**
     * Récupère les détails d'un produit.
     * 
     * @param productId Identifiant du produit
     * @return Result contenant le produit ou une erreur
     */
    suspend operator fun invoke(productId: String): Result<Product> {
        if (productId.isBlank()) {
            return Result.Error(
                com.project.e_commerce.domain.model.ApiError.ValidationError("Product ID cannot be empty")
            )
        }
        
        return productRepository.getProductById(productId)
    }
}
