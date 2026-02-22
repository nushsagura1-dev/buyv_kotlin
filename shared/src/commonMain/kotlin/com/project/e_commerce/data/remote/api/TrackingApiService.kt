package com.project.e_commerce.data.remote.api

import com.project.e_commerce.data.remote.dto.TrackClickRequestDto
import com.project.e_commerce.data.remote.dto.TrackConversionRequestDto
import com.project.e_commerce.data.remote.dto.TrackViewRequestDto
import com.project.e_commerce.data.remote.dto.TrackingResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Tracking API Service.
 * Handles marketplace analytics tracking endpoints.
 *
 * Endpoints:
 *  - POST /api/marketplace/track/view        → track reel view (optional auth)
 *  - POST /api/marketplace/track/click       → track affiliate click (optional auth)
 *  - POST /api/marketplace/track/conversion  → track conversion (auth required)
 */
class TrackingApiService(private val httpClient: HttpClient) {

    private val baseUrl = "api/marketplace/track"

    /**
     * Track a reel view.
     */
    suspend fun trackView(request: TrackViewRequestDto): TrackingResponseDto {
        return httpClient.post("$baseUrl/view") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Track an affiliate click on a product badge.
     */
    suspend fun trackClick(request: TrackClickRequestDto): TrackingResponseDto {
        return httpClient.post("$baseUrl/click") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Track a conversion (purchase after click).
     */
    suspend fun trackConversion(request: TrackConversionRequestDto): TrackingResponseDto {
        return httpClient.post("$baseUrl/conversion") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
