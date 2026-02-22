package com.project.e_commerce.domain.usecase.product

import com.project.e_commerce.domain.model.Category
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.ProductRepository

/**
 * Use case pour récupérer les catégories de produits.
 */
class GetCategoriesUseCase(
    private val productRepository: ProductRepository
) {
    /**
     * Récupère toutes les catégories.
     * 
     * @return Result contenant la liste de catégories
     */
    suspend fun getAllCategories(): Result<List<Category>> {
        return productRepository.getAllCategories()
    }

    /**
     * Opérateur invoke pour une utilisation simplifiée.
     */
    suspend operator fun invoke(): Result<List<Category>> {
        return getAllCategories()
    }
}
