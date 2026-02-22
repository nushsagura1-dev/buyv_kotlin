package com.project.e_commerce.android.data.repository

import android.content.Context
import android.os.Build
import com.project.e_commerce.android.data.api.TrackingApi
import com.project.e_commerce.android.data.api.TrackReelViewRequest
import com.project.e_commerce.android.data.api.TrackClickRequest
import com.project.e_commerce.android.data.api.TrackConversionRequest
import com.project.e_commerce.android.data.api.PromoterAnalyticsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Phase 6: Tracking Repository
 * Handles all tracking operations (views, clicks, conversions)
 */
class TrackingRepository(
    private val trackingApi: TrackingApi,
    private val context: Context,
    private val currentUserProvider: com.project.e_commerce.data.local.CurrentUserProvider
) {
    
    // Session ID persists for the app session (reset on app restart)
    private val sessionId: String by lazy {
        UUID.randomUUID().toString()
    }
    
    /**
     * Track Reel view/impression
     * Call when Reel is visible on screen for 1+ seconds
     * ✅ REACTIVATED: Now uses backend API via CurrentUserProvider
     */
    suspend fun trackReelView(
        reelId: String,
        promoterUid: String,
        productId: String? = null,
        watchDuration: Int? = null,
        completionRate: Float? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Get current user from CurrentUserProvider (backend auth)
            val currentUserId = currentUserProvider.getCurrentUserId()
            
            val request = TrackReelViewRequest(
                reel_id = reelId,
                promoter_uid = promoterUid,
                product_id = productId,
                viewer_uid = currentUserId,
                session_id = sessionId,
                watch_duration = watchDuration,
                completion_rate = completionRate
            )
            
            val response = trackingApi.trackReelView(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "View tracked")
            } else {
                Result.failure(Exception("Failed to track view: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track affiliate click on product badge
     * Call when user taps the marketplace product badge in a Reel
     * ✅ REACTIVATED: Now uses backend API via CurrentUserProvider
     */
    suspend fun trackAffiliateClick(
        reelId: String,
        productId: String,
        promoterUid: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Get current user from CurrentUserProvider (backend auth)
            val currentUserId = currentUserProvider.getCurrentUserId()
            
            val deviceInfo = mapOf(
                "device" to "Android",
                "os" to Build.VERSION.RELEASE,
                "sdk" to Build.VERSION.SDK_INT.toString(),
                "model" to Build.MODEL,
                "app_version" to getAppVersion()
            )
            
            val request = TrackClickRequest(
                reel_id = reelId,
                product_id = productId,
                promoter_uid = promoterUid,
                viewer_uid = currentUserId,
                session_id = sessionId,
                device_info = deviceInfo
            )
            
            val response = trackingApi.trackAffiliateClick(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Click tracked")
            } else {
                Result.failure(Exception("Failed to track click: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track conversion (purchase from affiliate link)
     * Call when order is placed after clicking affiliate link
     */
    suspend fun trackConversion(
        orderId: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = TrackConversionRequest(
                order_id = orderId,
                click_session_id = sessionId
            )
            
            val response = trackingApi.trackConversion(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.message ?: "Conversion tracked")
            } else {
                Result.failure(Exception("Failed to track conversion: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get analytics for a promoter
     * Shows views, clicks, conversions, earnings
     */
    suspend fun getPromoterAnalytics(
        promoterUid: String,
        days: Int = 30
    ): Result<PromoterAnalyticsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = trackingApi.getPromoterAnalytics(promoterUid, days)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get analytics: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ Helpers ============
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    /**
     * Get current session ID
     * Used to link clicks to conversions
     */
    fun getCurrentSessionId(): String = sessionId
}
