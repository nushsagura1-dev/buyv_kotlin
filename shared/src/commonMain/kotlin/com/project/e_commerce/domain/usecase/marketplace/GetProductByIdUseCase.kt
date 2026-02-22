package com.project.e_commerce.domain.usecase.marketplace

import com.project.e_commerce.data.repository.MarketplaceRepository
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.marketplace.MarketplaceProduct
import kotlinx.coroutines.flow.Flow

/**
 * Use case pour récupérer un produit par ID
 */
class GetProductByIdUseCase(
    private val repository: MarketplaceRepository
) {
    operator fun invoke(productId: String): Flow<Result<MarketplaceProduct>> {
        return repository.getProduct(productId)
    }
}
