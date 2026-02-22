package com.project.e_commerce.domain.usecase.tracking

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.TrackingRepository

/**
 * Use case to track a product click from a reel.
 */
class TrackClickUseCase(
    private val trackingRepository: TrackingRepository
) {
    suspend operator fun invoke(
        reelId: String,
        productId: String,
        promoterUid: String,
        viewerUid: String? = null,
        sessionId: String? = null,
        deviceInfo: String? = null
    ): Result<Unit> {
        if (reelId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Reel ID cannot be empty"))
        }
        if (productId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Product ID cannot be empty"))
        }
        if (promoterUid.isBlank()) {
            return Result.Error(ApiError.ValidationError("Promoter UID cannot be empty"))
        }
        return try {
            trackingRepository.trackClick(
                reelId = reelId,
                productId = productId,
                promoterUid = promoterUid,
                viewerUid = viewerUid,
                sessionId = sessionId,
                deviceInfo = deviceInfo
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to track click"))
        }
    }
}
