package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.OrderApiService
import com.project.e_commerce.data.remote.dto.OrderCreateRequest
import com.project.e_commerce.data.remote.dto.OrderItemCreateRequest
import com.project.e_commerce.data.remote.dto.ShippingAddressRequest
import com.project.e_commerce.data.remote.mapper.toDomain
import com.project.e_commerce.domain.model.*
import com.project.e_commerce.domain.repository.OrderRepository
import io.ktor.client.plugins.*
import io.ktor.utils.io.errors.*

/**
 * Network implementation of OrderRepository using FastAPI backend
 */
class OrderNetworkRepository(
    private val orderApi: OrderApiService
) : OrderRepository {
    
    override suspend fun createOrder(order: Order): Result<Order> {
        return try {
            // Build typed request DTO (kotlinx.serialization cannot serialize Map<String, Any?>)
            val orderRequest = OrderCreateRequest(
                orderNumber = order.orderNumber.ifBlank { null },
                items = order.items.map { item ->
                    OrderItemCreateRequest(
                        id = item.id.ifBlank { null },
                        productId = item.productId,
                        productName = item.productName,
                        productImage = item.productImage,
                        price = item.price,
                        quantity = item.quantity,
                        size = item.size,
                        color = item.color,
                        attributes = item.attributes,
                        isPromotedProduct = item.isPromotedProduct,
                        promoterId = item.promoterUid
                    )
                },
                status = order.status.name.lowercase(),
                subtotal = order.subtotal,
                shipping = order.shipping,
                tax = order.tax,
                total = order.total,
                shippingAddress = order.shippingAddress?.let { addr ->
                    ShippingAddressRequest(
                        fullName = addr.name,
                        address = addr.street,
                        city = addr.city,
                        state = addr.state,
                        zipCode = addr.zipCode,
                        country = addr.country,
                        phone = addr.phone
                    )
                },
                paymentMethod = order.paymentMethod,
                notes = order.notes,
                promoterId = order.promoterUid,
                paymentIntentId = order.paymentIntentId
            )
            
            val orderDto = orderApi.createOrder(orderRequest)
            Result.Success(orderDto.toDomain())
            
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val orderDto = orderApi.getOrderById(orderId)
            Result.Success(orderDto.toDomain())
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getOrdersByUser(userId: String): Result<List<Order>> {
        return try {
            val ordersDto = orderApi.getMyOrders(skip = 0, limit = 100)
            val orders = ordersDto.map { it.toDomain() }
            Result.Success(orders)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        return try {
            orderApi.updateOrderStatus(orderId, status.name.lowercase())
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else if (e.response.status.value == 403) {
                Result.Error(ApiError.Unauthorized)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> {
        return try {
            orderApi.cancelOrder(orderId, reason.ifBlank { null })
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getOrdersByStatus(userId: String, status: OrderStatus): Result<List<Order>> {
        return try {
            // Get all user orders and filter by status locally
            val ordersDto = orderApi.getMyOrders(skip = 0, limit = 100)
            val orders = ordersDto
                .map { it.toDomain() }
                .filter { it.status == status }
            Result.Success(orders)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getRecentOrders(userId: String, limit: Int): Result<List<Order>> {
        return try {
            val ordersDto = orderApi.getMyOrders(skip = 0, limit = limit)
            val orders = ordersDto.map { it.toDomain() }
            Result.Success(orders)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun canCancelOrder(orderId: String): Result<Boolean> {
        return try {
            // Vérifier si la commande peut être annulée (statut = PENDING ou PROCESSING)
            val orderDto = orderApi.getOrderById(orderId)
            val order = orderDto.toDomain()
            val canCancel = order.status == OrderStatus.PENDING || order.status == OrderStatus.PROCESSING
            Result.Success(canCancel)
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
}
