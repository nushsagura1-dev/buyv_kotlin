package com.project.e_commerce.domain.usecase.marketplace

import com.project.e_commerce.data.repository.MarketplaceRepository
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.marketplace.PromoterWallet
import kotlinx.coroutines.flow.Flow

/**
 * Use case pour récupérer le portefeuille promoteur
 */
class GetMyWalletUseCase(
    private val repository: MarketplaceRepository
) {
    operator fun invoke(): Flow<Result<PromoterWallet>> {
        return repository.getMyWallet()
    }
}
