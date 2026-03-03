package com.project.e_commerce.android.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Phase 6: Tracking API
 * Handles tracking of Reel views, affiliate clicks, and conversions
 */
class TrackingApi(private val httpClient: HttpClient) {

    // ============ Tracking Events ============

    suspend fun trackReelView(request: TrackReelViewRequest): TrackingResponse =
        httpClient.post("api/marketplace/track/view") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun trackAffiliateClick(request: TrackClickRequest): TrackingResponse =
        httpClient.post("api/marketplace/track/click") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun trackConversion(request: TrackConversionRequest): TrackingResponse =
        httpClient.post("api/marketplace/track/conversion") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ============ Analytics ============

    suspend fun getPromoterAnalytics(
        promoterUid: String,
        days: Int = 30
    ): PromoterAnalyticsResponse =
        httpClient.get("api/marketplace/analytics/promoter/$promoterUid") {
            parameter("days", days)
        }.body()
}

// ============ Request Models ============

@Serializable
data class TrackReelViewRequest(
    val reel_id: String,
    val promoter_uid: String,
    val product_id: String? = null,
    val viewer_uid: String? = null,  // null for anonymous
    val session_id: String? = null,
    val watch_duration: Int? = null,  // seconds
    val completion_rate: Float? = null  // 0.0 to 1.0
)

@Serializable
data class TrackClickRequest(
    val reel_id: String,
    val product_id: String,
    val promoter_uid: String,
    val viewer_uid: String? = null,
    val session_id: String? = null,
    val device_info: Map<String, String>? = null
)

@Serializable
data class TrackConversionRequest(
    val order_id: Int,
    val click_session_id: String
)

// ============ Response Models ============

@Serializable
data class TrackingResponse(
    val success: Boolean,
    val message: String,
    val tracking_id: Int? = null
)

@Serializable
data class PromoterAnalyticsResponse(
    val promoter_uid: String,
    val period_days: Int,
    val metrics: AnalyticsMetrics,
    val earnings: EarningsData,
    val stats: PromoterStats
)

@Serializable
data class AnalyticsMetrics(
    val views: Int,
    val clicks: Int,
    val conversions: Int,
    val ctr: Double,  // Click-through rate
    val conversion_rate: Double
)

@Serializable
data class EarningsData(
    val total_earned: Double,
    val pending_balance: Double,
    val available_balance: Double,
    val withdrawn_total: Double,
    val pending_commissions: Double,
    val approved_commissions: Double
)

@Serializable
data class PromoterStats(
    val total_sales: Int,
    val avg_commission_per_sale: Double
)
