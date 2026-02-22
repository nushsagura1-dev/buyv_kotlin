package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.TrackingApiService
import com.project.e_commerce.data.remote.dto.TrackClickRequestDto
import com.project.e_commerce.data.remote.dto.TrackConversionRequestDto
import com.project.e_commerce.data.remote.dto.TrackViewRequestDto
import com.project.e_commerce.domain.repository.TrackingRepository

/**
 * Network implementation of TrackingRepository.
 */
class TrackingNetworkRepository(
    private val trackingApiService: TrackingApiService
) : TrackingRepository {

    override suspend fun trackView(
        reelId: String,
        promoterUid: String,
        productId: String?,
        viewerUid: String?,
        sessionId: String?,
        watchDuration: Int?,
        completionRate: Double?
    ) {
        trackingApiService.trackView(
            TrackViewRequestDto(
                reelId = reelId,
                promoterUid = promoterUid,
                productId = productId,
                viewerUid = viewerUid,
                sessionId = sessionId,
                watchDuration = watchDuration,
                completionRate = completionRate
            )
        )
    }

    override suspend fun trackClick(
        reelId: String,
        productId: String,
        promoterUid: String,
        viewerUid: String?,
        sessionId: String?,
        deviceInfo: String?
    ) {
        trackingApiService.trackClick(
            TrackClickRequestDto(
                reelId = reelId,
                productId = productId,
                promoterUid = promoterUid,
                viewerUid = viewerUid,
                sessionId = sessionId,
                deviceInfo = deviceInfo
            )
        )
    }

    override suspend fun trackConversion(orderId: String, clickSessionId: String) {
        trackingApiService.trackConversion(
            TrackConversionRequestDto(
                orderId = orderId,
                clickSessionId = clickSessionId
            )
        )
    }
}
