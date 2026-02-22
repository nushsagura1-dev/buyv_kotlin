package com.project.e_commerce.domain.usecase.order

import com.project.e_commerce.domain.model.*
import com.project.e_commerce.domain.repository.OrderRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UseCase tests for order domain: Create, GetByUser, GetRecent, GetDetails, Cancel.
 *
 * Uses hand-written FakeOrderRepository.
 */

// ── Fake Repository ────────────────────────────────────────
private class FakeOrderRepository(
    var createResult: Result<Order> = Result.Success(Order(id = "o1")),
    var getByUserResult: Result<List<Order>> = Result.Success(emptyList()),
    var getByIdResult: Result<Order> = Result.Success(Order(id = "o1")),
    var cancelResult: Result<Unit> = Result.Success(Unit),
    var canCancelResult: Result<Boolean> = Result.Success(true),
    var recentResult: Result<List<Order>> = Result.Success(emptyList()),
) : OrderRepository {
    var lastCreatedOrder: Order? = null
    var lastCancelledOrderId: String? = null
    var lastCancelReason: String? = null

    override suspend fun createOrder(order: Order): Result<Order> {
        lastCreatedOrder = order; return createResult
    }

    override suspend fun getOrdersByUser(userId: String): Result<List<Order>> = getByUserResult
    override suspend fun getOrderById(orderId: String): Result<Order> = getByIdResult
    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = Result.Success(Unit)

    override suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> {
        lastCancelledOrderId = orderId; lastCancelReason = reason
        return cancelResult
    }

    override suspend fun getOrdersByStatus(userId: String, status: OrderStatus): Result<List<Order>> =
        Result.Success(emptyList())

    override suspend fun getRecentOrders(userId: String, limit: Int): Result<List<Order>> = recentResult
    override suspend fun canCancelOrder(orderId: String): Result<Boolean> = canCancelResult
}


// ════════════════════════════════════════════════
// CREATE ORDER USE CASE
// ════════════════════════════════════════════════

class CreateOrderUseCaseTest {

    private val validOrder = Order(
        id = "", userId = "u1",
        items = listOf(OrderItem(productId = "p1", productName = "Test", price = 10.0, quantity = 2)),
        total = 20.0
    )

    @Test
    fun create_order_success() = runTest {
        val repo = FakeOrderRepository(createResult = Result.Success(validOrder.copy(id = "o99")))
        val result = CreateOrderUseCase(repo)(validOrder)

        assertIs<Result.Success<Order>>(result)
        assertEquals("o99", result.data.id)
    }

    @Test
    fun create_order_delegates_to_repo() = runTest {
        val repo = FakeOrderRepository()
        CreateOrderUseCase(repo)(validOrder)

        assertEquals("u1", repo.lastCreatedOrder?.userId)
        assertEquals(1, repo.lastCreatedOrder?.items?.size)
    }

    @Test
    fun create_order_empty_items_returns_validation_error() = runTest {
        val repo = FakeOrderRepository()
        val emptyOrder = validOrder.copy(items = emptyList())
        val result = CreateOrderUseCase(repo)(emptyOrder)

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("item", ignoreCase = true))
    }

    @Test
    fun create_order_zero_total_returns_validation_error() = runTest {
        val repo = FakeOrderRepository()
        val zeroOrder = validOrder.copy(total = 0.0)
        val result = CreateOrderUseCase(repo)(zeroOrder)

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("total", ignoreCase = true))
    }

    @Test
    fun create_order_negative_total_returns_validation_error() = runTest {
        val repo = FakeOrderRepository()
        val negOrder = validOrder.copy(total = -5.0)
        val result = CreateOrderUseCase(repo)(negOrder)

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
    }

    @Test
    fun create_order_server_error_propagates() = runTest {
        val repo = FakeOrderRepository(createResult = Result.Error(ApiError.ServerError))
        val result = CreateOrderUseCase(repo)(validOrder)

        assertIs<Result.Error>(result)
        assertIs<ApiError.ServerError>(result.error)
    }
}


// ════════════════════════════════════════════════
// GET ORDERS BY USER USE CASE
// ════════════════════════════════════════════════

class GetOrdersByUserUseCaseTest {

    @Test
    fun get_orders_success() = runTest {
        val orders = listOf(Order(id = "o1"), Order(id = "o2"))
        val repo = FakeOrderRepository(getByUserResult = Result.Success(orders))
        val result = GetOrdersByUserUseCase(repo)("user1")

        assertIs<Result.Success<List<Order>>>(result)
        assertEquals(2, result.data.size)
    }

    @Test
    fun get_orders_empty_user_id_returns_validation_error() = runTest {
        val repo = FakeOrderRepository()
        val result = GetOrdersByUserUseCase(repo)("")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("User ID", ignoreCase = true))
    }

    @Test
    fun get_orders_blank_user_id_returns_validation_error() = runTest {
        val repo = FakeOrderRepository()
        val result = GetOrdersByUserUseCase(repo)("   ")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
    }

    @Test
    fun get_orders_network_error_propagates() = runTest {
        val repo = FakeOrderRepository(getByUserResult = Result.Error(ApiError.NetworkError))
        val result = GetOrdersByUserUseCase(repo)("user1")

        assertIs<Result.Error>(result)
        assertIs<ApiError.NetworkError>(result.error)
    }
}


// ════════════════════════════════════════════════
// GET RECENT ORDERS USE CASE
// ════════════════════════════════════════════════

class GetRecentOrdersUseCaseTest {

    @Test
    fun get_recent_orders_success() = runTest {
        val orders = listOf(Order(id = "r1"), Order(id = "r2"))
        val repo = FakeOrderRepository(recentResult = Result.Success(orders))
        val result = GetRecentOrdersUseCase(repo)("user1")

        assertIs<Result.Success<List<Order>>>(result)
        assertEquals(2, result.data.size)
    }

    @Test
    fun get_recent_blank_user_id_returns_error() = runTest {
        val repo = FakeOrderRepository()
        val result = GetRecentOrdersUseCase(repo)("")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
    }

    @Test
    fun get_recent_zero_limit_returns_error() = runTest {
        val repo = FakeOrderRepository()
        val result = GetRecentOrdersUseCase(repo)("user1", 0)

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("Limit", ignoreCase = true))
    }

    @Test
    fun get_recent_negative_limit_returns_error() = runTest {
        val repo = FakeOrderRepository()
        val result = GetRecentOrdersUseCase(repo)("user1", -3)

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
    }
}


// ════════════════════════════════════════════════
// GET ORDER DETAILS USE CASE
// ════════════════════════════════════════════════

class GetOrderDetailsUseCaseTest {

    @Test
    fun get_details_success() = runTest {
        val order = Order(id = "o5", userId = "u1", total = 99.99)
        val repo = FakeOrderRepository(getByIdResult = Result.Success(order))
        val result = GetOrderDetailsUseCase(repo)("o5")

        assertIs<Result.Success<Order>>(result)
        assertEquals("o5", result.data.id)
        assertEquals(99.99, result.data.total)
    }

    @Test
    fun get_details_blank_id_returns_error() = runTest {
        val repo = FakeOrderRepository()
        val result = GetOrderDetailsUseCase(repo)("")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("Order ID", ignoreCase = true))
    }

    @Test
    fun get_details_not_found() = runTest {
        val repo = FakeOrderRepository(getByIdResult = Result.Error(ApiError.NotFound))
        val result = GetOrderDetailsUseCase(repo)("nonexistent")

        assertIs<Result.Error>(result)
        assertIs<ApiError.NotFound>(result.error)
    }
}


// ════════════════════════════════════════════════
// CANCEL ORDER USE CASE
// ════════════════════════════════════════════════

class CancelOrderUseCaseTest {

    @Test
    fun cancel_order_success() = runTest {
        val repo = FakeOrderRepository(
            canCancelResult = Result.Success(true),
            cancelResult = Result.Success(Unit),
        )
        val result = CancelOrderUseCase(repo)("o1", "Changed my mind")

        assertIs<Result.Success<Unit>>(result)
        assertEquals("o1", repo.lastCancelledOrderId)
        assertEquals("Changed my mind", repo.lastCancelReason)
    }

    @Test
    fun cancel_order_blank_id_returns_error() = runTest {
        val repo = FakeOrderRepository()
        val result = CancelOrderUseCase(repo)("", "reason")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("Order ID", ignoreCase = true))
    }

    @Test
    fun cancel_order_blank_reason_returns_error() = runTest {
        val repo = FakeOrderRepository()
        val result = CancelOrderUseCase(repo)("o1", "")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("reason", ignoreCase = true))
    }

    @Test
    fun cancel_order_not_cancellable() = runTest {
        val repo = FakeOrderRepository(canCancelResult = Result.Success(false))
        val result = CancelOrderUseCase(repo)("o1", "reason")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("cannot be cancelled", ignoreCase = true))
    }

    @Test
    fun cancel_order_canCancel_error_propagates() = runTest {
        val repo = FakeOrderRepository(canCancelResult = Result.Error(ApiError.NetworkError))
        val result = CancelOrderUseCase(repo)("o1", "reason")

        assertIs<Result.Error>(result)
        assertIs<ApiError.NetworkError>(result.error)
    }
}
