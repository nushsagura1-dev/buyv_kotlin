package com.project.e_commerce.domain.usecase.marketplace

import com.project.e_commerce.data.repository.MarketplaceRepository
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.marketplace.ProductPromotion
import kotlinx.coroutines.flow.Flow

/**
 * Use case pour créer une promotion (lier post → produit)
 */
class CreatePromotionUseCase(
    private val repository: MarketplaceRepository
) {
    operator fun invoke(
        postId: String,
        productId: String
    ): Flow<Result<ProductPromotion>> {
        return repository.createPromotion(postId, productId)
    }
}
