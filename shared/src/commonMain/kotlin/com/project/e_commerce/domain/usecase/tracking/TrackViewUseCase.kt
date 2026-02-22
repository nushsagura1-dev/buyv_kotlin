package com.project.e_commerce.domain.usecase.tracking

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.TrackingRepository

/**
 * Use case to track a reel view.
 */
class TrackViewUseCase(
    private val trackingRepository: TrackingRepository
) {
    suspend operator fun invoke(
        reelId: String,
        promoterUid: String,
        productId: String? = null,
        viewerUid: String? = null,
        sessionId: String? = null,
        watchDuration: Int? = null,
        completionRate: Double? = null
    ): Result<Unit> {
        if (reelId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Reel ID cannot be empty"))
        }
        if (promoterUid.isBlank()) {
            return Result.Error(ApiError.ValidationError("Promoter UID cannot be empty"))
        }
        return try {
            trackingRepository.trackView(
                reelId = reelId,
                promoterUid = promoterUid,
                productId = productId,
                viewerUid = viewerUid,
                sessionId = sessionId,
                watchDuration = watchDuration,
                completionRate = completionRate
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to track view"))
        }
    }
}
