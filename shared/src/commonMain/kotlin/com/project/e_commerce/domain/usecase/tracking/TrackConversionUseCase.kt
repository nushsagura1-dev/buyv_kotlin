package com.project.e_commerce.domain.usecase.tracking

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.TrackingRepository

/**
 * Use case to track an order conversion.
 */
class TrackConversionUseCase(
    private val trackingRepository: TrackingRepository
) {
    suspend operator fun invoke(
        orderId: String,
        clickSessionId: String
    ): Result<Unit> {
        if (orderId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Order ID cannot be empty"))
        }
        if (clickSessionId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Click session ID cannot be empty"))
        }
        return try {
            trackingRepository.trackConversion(
                orderId = orderId,
                clickSessionId = clickSessionId
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to track conversion"))
        }
    }
}
