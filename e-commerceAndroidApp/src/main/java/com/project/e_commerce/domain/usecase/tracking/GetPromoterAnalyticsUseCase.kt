package com.project.e_commerce.domain.usecase.tracking

import com.project.e_commerce.android.data.api.PromoterAnalyticsResponse
import com.project.e_commerce.android.data.repository.TrackingRepository

/**
 * Phase 7: Use Case pour récupérer les analytics d'un promoteur
 * Retourne les métriques, earnings et stats
 */
class GetPromoterAnalyticsUseCase(
    private val trackingRepository: TrackingRepository
) {
    
    /**
     * Récupère les analytics pour un promoteur
     * @param promoterUid UID du promoteur (Firebase user ID)
     * @param days Nombre de jours à inclure (default 30)
     * @return Result<PromoterAnalyticsResponse> avec toutes les métriques
     */
    suspend operator fun invoke(
        promoterUid: String,
        days: Int = 30
    ): Result<PromoterAnalyticsResponse> {
        return trackingRepository.getPromoterAnalytics(promoterUid, days)
    }
}
