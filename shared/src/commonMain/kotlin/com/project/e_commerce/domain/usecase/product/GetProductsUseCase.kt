package com.project.e_commerce.domain.usecase.product

import com.project.e_commerce.domain.model.Product
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.ProductRepository

/**
 * Use case pour récupérer une liste de produits avec filtres optionnels.
 * 
 * Centralise la logique de récupération des produits pour l'UI.
 */
class GetProductsUseCase(
    private val productRepository: ProductRepository
) {
    /**
     * Récupère tous les produits.
     * 
     * @return Result contenant la liste de produits
     */
    suspend fun getAllProducts(): Result<List<Product>> {
        return productRepository.getAllProducts()
    }
    
    /**
     * Récupère les produits d'une catégorie.
     * 
     * @param categoryId Identifiant de la catégorie
     * @return Result contenant la liste de produits filtrés
     */
    suspend fun getProductsByCategory(categoryId: String): Result<List<Product>> {
        return productRepository.getProductsByCategory(categoryId)
    }
    
    /**
     * Récupère les produits populaires.
     * 
     * @param limit Nombre maximum de produits
     * @return Result contenant les produits populaires
     */
    suspend fun getPopularProducts(limit: Int = 10): Result<List<Product>> {
        return productRepository.getPopularProducts(limit)
    }
    
    /**
     * Récupère les produits récents.
     * 
     * @param limit Nombre maximum de produits
     * @return Result contenant les produits récents
     */
    suspend fun getRecentProducts(limit: Int = 10): Result<List<Product>> {
        return productRepository.getRecentProducts(limit)
    }
    
    /**
     * Recherche des produits par mots-clés.
     * 
     * @param query Requête de recherche
     * @return Result contenant les produits correspondants
     */
    suspend fun searchProducts(query: String): Result<List<Product>> {
        if (query.isBlank()) {
            return getAllProducts()
        }
        return productRepository.searchProducts(query)
    }
    
    /**
     * Récupère les meilleurs ventes (featured products).
     * Pour l'instant, retourne les produits populaires.
     * 
     * @return Result contenant les best sellers
     */
    suspend fun getBestSellers(): Result<List<Product>> {
        return getPopularProducts(20)
    }
    
    /**
     * Opérateur invoke pour une utilisation simplifiée (tous les produits).
     */
    suspend operator fun invoke(): Result<List<Product>> {
        return getAllProducts()
    }
}
