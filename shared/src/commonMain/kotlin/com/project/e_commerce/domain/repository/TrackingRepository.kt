package com.project.e_commerce.domain.repository

/**
 * Repository interface for marketplace tracking/analytics operations.
 */
interface TrackingRepository {
    /** Track a reel view. */
    suspend fun trackView(
        reelId: String,
        promoterUid: String,
        productId: String? = null,
        viewerUid: String? = null,
        sessionId: String? = null,
        watchDuration: Int? = null,
        completionRate: Double? = null
    )

    /** Track an affiliate click on a product. */
    suspend fun trackClick(
        reelId: String,
        productId: String,
        promoterUid: String,
        viewerUid: String? = null,
        sessionId: String? = null,
        deviceInfo: String? = null
    )

    /** Track a conversion (purchase after click). */
    suspend fun trackConversion(orderId: String, clickSessionId: String)
}
