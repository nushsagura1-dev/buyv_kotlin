package com.project.e_commerce.android.presentation.viewModel

import com.project.e_commerce.android.domain.usecase.CancelOrderUseCase
import com.project.e_commerce.android.domain.usecase.GetUserOrdersUseCase
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.OrderItem
import com.project.e_commerce.domain.model.OrderStatus
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for OrderHistoryViewModel.
 * Covers: loadUserOrders, cancelOrder, tab filtering, error states.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var getUserOrders: GetUserOrdersUseCase
    private lateinit var cancelOrder: CancelOrderUseCase
    private lateinit var currentUserProvider: CurrentUserProvider

    // Sample data
    private val sampleOrders = listOf(
        Order(id = "o1", userId = "user1", status = OrderStatus.PENDING, total = 50.0),
        Order(id = "o2", userId = "user1", status = OrderStatus.DELIVERED, total = 30.0),
        Order(id = "o3", userId = "user1", status = OrderStatus.CANCELED, total = 20.0),
        Order(id = "o4", userId = "user1", status = OrderStatus.PROCESSING, total = 40.0),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getUserOrders = mockk()
        cancelOrder = mockk()
        currentUserProvider = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OrderHistoryViewModel {
        return OrderHistoryViewModel(getUserOrders, cancelOrder, currentUserProvider)
    }

    // ════════════════════════════════════════════
    // LOAD USER ORDERS
    // ════════════════════════════════════════════

    @Test
    fun `init loads orders for authenticated user`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(4, state.allOrders.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `load orders sets error when user not authenticated`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns null

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("not authenticated", ignoreCase = true))
    }

    @Test
    fun `load orders handles exception`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } throws RuntimeException("Network error")

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network error"))
        assertFalse(state.isLoading)
    }

    @Test
    fun `load empty order list`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(emptyList())

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.allOrders.isEmpty())
        assertTrue(state.filteredOrders.isEmpty())
    }

    @Test
    fun `order counts are calculated correctly`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        val counts = vm.state.value.orderCounts
        assertEquals(4, counts["All"])
        assertEquals(1, counts["Completed"]) // DELIVERED
        assertEquals(2, counts["Pending"])    // PENDING + PROCESSING
        assertEquals(1, counts["Canceled"])   // CANCELED
    }


    // ════════════════════════════════════════════
    // TAB FILTERING
    // ════════════════════════════════════════════

    @Test
    fun `tab 0 All shows all orders`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTabSelected(0)
        assertEquals(4, vm.state.value.filteredOrders.size)
    }

    @Test
    fun `tab 1 Completed shows delivered only`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTabSelected(1)
        val filtered = vm.state.value.filteredOrders
        assertEquals(1, filtered.size)
        assertEquals(OrderStatus.DELIVERED, filtered.first().status)
    }

    @Test
    fun `tab 2 Pending shows pending and processing`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTabSelected(2)
        val filtered = vm.state.value.filteredOrders
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.status in listOf(OrderStatus.PENDING, OrderStatus.PROCESSING) })
    }

    @Test
    fun `tab 3 Canceled shows canceled`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTabSelected(3)
        val filtered = vm.state.value.filteredOrders
        assertEquals(1, filtered.size)
        assertEquals(OrderStatus.CANCELED, filtered.first().status)
    }

    @Test
    fun `invalid tab index does nothing`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTabSelected(99)
        assertEquals(0, vm.state.value.selectedTabIndex) // unchanged
    }

    @Test
    fun `getTabsWithCounts returns correct pairs`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        val tabs = vm.getTabsWithCounts()
        assertEquals(4, tabs.size)
        assertEquals("All" to 4, tabs[0])
        assertEquals("Completed" to 1, tabs[1])
        assertEquals("Pending" to 2, tabs[2])
        assertEquals("Canceled" to 1, tabs[3])
    }


    // ════════════════════════════════════════════
    // CANCEL ORDER
    // ════════════════════════════════════════════

    @Test
    fun `cancel order success`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)
        coEvery { cancelOrder("o1") } returns kotlin.Result.success(Unit)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.cancelOrder("o1")
        advanceUntilIdle()

        coVerify { cancelOrder("o1") }
        // No error in state after successful cancel
        assertNull(vm.state.value.error)
    }

    @Test
    fun `cancel order failure sets error`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)
        coEvery { cancelOrder("o1") } returns kotlin.Result.failure(Exception("Already shipped"))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.cancelOrder("o1")
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.error!!.contains("Already shipped"))
    }


    // ════════════════════════════════════════════
    // UTILITY
    // ════════════════════════════════════════════

    @Test
    fun `clearError clears error in state`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns null

        val vm = createViewModel()
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        vm.clearError()
        assertNull(vm.state.value.error)
    }

    @Test
    fun `refreshOrders reloads data`() = runTest {
        coEvery { currentUserProvider.getCurrentUserId() } returns "user1"
        every { getUserOrders("user1") } returns flowOf(sampleOrders)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.refreshOrders()
        advanceUntilIdle()

        // getUserOrders called twice: init + refresh
        verify(atLeast = 2) { getUserOrders("user1") }
    }
}
