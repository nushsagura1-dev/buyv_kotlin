package com.project.e_commerce.domain.usecase.marketplace

import com.project.e_commerce.data.remote.api.ProductListResponse
import com.project.e_commerce.data.repository.MarketplaceRepository
import com.project.e_commerce.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Use case pour récupérer les produits du marketplace
 */
class GetProductsUseCase(
    private val repository: MarketplaceRepository
) {
    operator fun invoke(
        categoryId: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minCommission: Double? = null,
        search: String? = null,
        sortBy: String = "relevance",
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<ProductListResponse>> {
        return repository.getProducts(
            categoryId, minPrice, maxPrice, minCommission,
            search, sortBy, page, limit
        )
    }
}
