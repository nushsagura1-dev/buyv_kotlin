package com.project.e_commerce.android.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Phase 6: Tracking API
 * Handles tracking of Reel views, affiliate clicks, and conversions
 */
interface TrackingApi {
    
    // ============ Tracking Events ============
    
    @POST("api/marketplace/track/view")
    suspend fun trackReelView(@Body request: TrackReelViewRequest): Response<TrackingResponse>
    
    @POST("api/marketplace/track/click")
    suspend fun trackAffiliateClick(@Body request: TrackClickRequest): Response<TrackingResponse>
    
    @POST("api/marketplace/track/conversion")
    suspend fun trackConversion(@Body request: TrackConversionRequest): Response<TrackingResponse>
    
    // ============ Analytics ============
    
    @GET("api/marketplace/analytics/promoter/{promoter_uid}")
    suspend fun getPromoterAnalytics(
        @Path("promoter_uid") promoterUid: String,
        @Query("days") days: Int = 30
    ): Response<PromoterAnalyticsResponse>
}

// ============ Request Models ============

data class TrackReelViewRequest(
    val reel_id: String,
    val promoter_uid: String,
    val product_id: String? = null,
    val viewer_uid: String? = null,  // null for anonymous
    val session_id: String? = null,
    val watch_duration: Int? = null,  // seconds
    val completion_rate: Float? = null  // 0.0 to 1.0
)

data class TrackClickRequest(
    val reel_id: String,
    val product_id: String,
    val promoter_uid: String,
    val viewer_uid: String? = null,
    val session_id: String? = null,
    val device_info: Map<String, String>? = null
)

data class TrackConversionRequest(
    val order_id: Int,
    val click_session_id: String
)

// ============ Response Models ============

data class TrackingResponse(
    val success: Boolean,
    val message: String,
    val tracking_id: Int? = null
)

data class PromoterAnalyticsResponse(
    val promoter_uid: String,
    val period_days: Int,
    val metrics: AnalyticsMetrics,
    val earnings: EarningsData,
    val stats: PromoterStats
)

data class AnalyticsMetrics(
    val views: Int,
    val clicks: Int,
    val conversions: Int,
    val ctr: Double,  // Click-through rate
    val conversion_rate: Double
)

data class EarningsData(
    val total_earned: Double,
    val pending_balance: Double,
    val available_balance: Double,
    val withdrawn_total: Double,
    val pending_commissions: Double,
    val approved_commissions: Double
)

data class PromoterStats(
    val total_sales: Int,
    val avg_commission_per_sale: Double
)
