package com.project.e_commerce.data.remote.api

import com.project.e_commerce.data.remote.dto.OrderCreateRequest
import com.project.e_commerce.data.remote.dto.OrderDto
import com.project.e_commerce.data.remote.dto.MessageResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Order API service
 * Handles order management endpoints
 */
class OrderApiService(private val httpClient: HttpClient) {
    
    /**
     * Create a new order
     * POST /orders
     */
    suspend fun createOrder(orderData: OrderCreateRequest): OrderDto {
        return httpClient.post("orders") {
            contentType(ContentType.Application.Json)
            setBody(orderData)
        }.body()
    }
    
    /**
     * Get current user's orders
     * GET /orders/me?skip={skip}&limit={limit}
     */
    suspend fun getMyOrders(skip: Int = 0, limit: Int = 20): List<OrderDto> {
        return httpClient.get("orders/me") {
            parameter("skip", skip)
            parameter("limit", limit)
        }.body()
    }
    
    /**
     * Get specific order by ID
     * GET /orders/{order_id}
     */
    suspend fun getOrderById(orderId: String): OrderDto {
        return httpClient.get("orders/$orderId").body()
    }
    
    /**
     * Cancel an order
     * POST /orders/{order_id}/cancel
     */
    suspend fun cancelOrder(orderId: String, reason: String? = null): MessageResponseDto {
        return httpClient.post("orders/$orderId/cancel") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("reason" to reason))
        }.body()
    }
    
    /**
     * Update order status (admin/seller)
     * PUT /orders/{order_id}/status
     */
    suspend fun updateOrderStatus(orderId: String, status: String): OrderDto {
        return httpClient.put("orders/$orderId/status") {
            setBody(mapOf("status" to status))
        }.body()
    }
}
