package com.project.e_commerce.android.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sprint 21: Unit tests for Admin API data classes.
 * Tests construction, defaults, copy, and equality of all data classes
 * added to AdminApi.kt during Sprint 20-21.
 */
class AdminApiDataClassesTest {

    // ========== AdminCategoryResponse ==========

    @Test
    fun `AdminCategoryResponse - full construction`() {
        val cat = AdminCategoryResponse(
            id = "cat-1",
            name = "Electronics",
            name_ar = "إلكترونيات",
            slug = "electronics",
            icon_url = "https://img.co/elec.png",
            parent_id = null,
            display_order = 1,
            is_active = true,
            created_at = "2025-01-01T00:00:00Z"
        )
        assertEquals("cat-1", cat.id)
        assertEquals("Electronics", cat.name)
        assertEquals("إلكترونيات", cat.name_ar)
        assertEquals("electronics", cat.slug)
        assertEquals("https://img.co/elec.png", cat.icon_url)
        assertNull(cat.parent_id)
        assertEquals(1, cat.display_order)
        assertTrue(cat.is_active)
    }

    @Test
    fun `AdminCategoryResponse - equality and copy`() {
        val cat1 = AdminCategoryResponse(
            id = "1", name = "A", name_ar = null, slug = "a",
            icon_url = null, parent_id = null, display_order = 0,
            is_active = true, created_at = "2025-01-01"
        )
        val cat2 = cat1.copy()
        assertEquals(cat1, cat2)

        val cat3 = cat1.copy(is_active = false)
        assertFalse(cat3.is_active)
        assertTrue(cat1.is_active)
    }

    // ========== CategoryCreateRequest ==========

    @Test
    fun `CategoryCreateRequest - full construction`() {
        val req = CategoryCreateRequest(
            name = "Fashion",
            name_ar = "أزياء",
            slug = "fashion",
            icon_url = "https://img.co/fashion.png",
            parent_id = "parent-1",
            display_order = 5,
            is_active = true
        )
        assertEquals("Fashion", req.name)
        assertEquals("أزياء", req.name_ar)
        assertEquals("fashion", req.slug)
        assertEquals("https://img.co/fashion.png", req.icon_url)
        assertEquals("parent-1", req.parent_id)
        assertEquals(5, req.display_order)
        assertTrue(req.is_active)
    }

    @Test
    fun `CategoryCreateRequest - nullable fields can be null`() {
        val req = CategoryCreateRequest(
            name = "Test",
            name_ar = null,
            slug = "test",
            icon_url = null,
            parent_id = null,
            display_order = 0,
            is_active = false
        )
        assertNull(req.name_ar)
        assertNull(req.icon_url)
        assertNull(req.parent_id)
        assertFalse(req.is_active)
    }

    // ========== CategoryUpdateRequest ==========

    @Test
    fun `CategoryUpdateRequest - all null for partial update`() {
        val req = CategoryUpdateRequest(
            name = null,
            name_ar = null,
            icon_url = null,
            display_order = null,
            is_active = null
        )
        assertNull(req.name)
        assertNull(req.name_ar)
        assertNull(req.icon_url)
        assertNull(req.display_order)
        assertNull(req.is_active)
    }

    @Test
    fun `CategoryUpdateRequest - partial fields`() {
        val req = CategoryUpdateRequest(
            name = "Updated Name",
            name_ar = null,
            icon_url = null,
            display_order = null,
            is_active = false
        )
        assertEquals("Updated Name", req.name)
        assertEquals(false, req.is_active)
        assertNull(req.display_order)
    }

    // ========== AdminAffiliateSaleResponse ==========

    @Test
    fun `AdminAffiliateSaleResponse - full construction`() {
        val sale = AdminAffiliateSaleResponse(
            id = "sale-1",
            order_id = "ord-100",
            product_id = "prod-50",
            promotion_id = "promo-10",
            buyer_user_id = "buyer-1",
            promoter_user_id = "promoter-1",
            sale_amount = 99.99,
            product_price = 49.99,
            quantity = 2,
            commission_rate = 10.0,
            commission_amount = 9.99,
            commission_status = "pending",
            paid_at = null,
            payment_reference = null,
            created_at = "2025-02-01T10:30:00Z"
        )
        assertEquals("sale-1", sale.id)
        assertEquals("ord-100", sale.order_id)
        assertEquals(99.99, sale.sale_amount, 0.001)
        assertEquals(2, sale.quantity)
        assertEquals(10.0, sale.commission_rate, 0.001)
        assertEquals(9.99, sale.commission_amount, 0.001)
        assertEquals("pending", sale.commission_status)
        assertNull(sale.paid_at)
        assertNull(sale.payment_reference)
    }

    @Test
    fun `AdminAffiliateSaleResponse - paid sale`() {
        val sale = AdminAffiliateSaleResponse(
            id = "sale-2",
            order_id = "ord-200",
            product_id = "prod-60",
            promotion_id = null,
            buyer_user_id = "buyer-2",
            promoter_user_id = "promoter-2",
            sale_amount = 50.0,
            product_price = 50.0,
            quantity = 1,
            commission_rate = 15.0,
            commission_amount = 7.5,
            commission_status = "paid",
            paid_at = "2025-02-15T12:00:00Z",
            payment_reference = "PAY-12345",
            created_at = "2025-02-01T10:30:00Z"
        )
        assertEquals("paid", sale.commission_status)
        assertEquals("2025-02-15T12:00:00Z", sale.paid_at)
        assertEquals("PAY-12345", sale.payment_reference)
    }

    @Test
    fun `AdminAffiliateSaleResponse - equality`() {
        val sale1 = AdminAffiliateSaleResponse(
            id = "1", order_id = "o1", product_id = "p1",
            promotion_id = null, buyer_user_id = "b1", promoter_user_id = "pr1",
            sale_amount = 100.0, product_price = 100.0, quantity = 1,
            commission_rate = 10.0, commission_amount = 10.0,
            commission_status = "pending", paid_at = null,
            payment_reference = null, created_at = "2025-01-01"
        )
        val sale2 = sale1.copy()
        assertEquals(sale1, sale2)
    }

    // ========== SaleStatusUpdateRequest ==========

    @Test
    fun `SaleStatusUpdateRequest - approve`() {
        val req = SaleStatusUpdateRequest(status = "approved")
        assertEquals("approved", req.status)
        assertNull(req.payment_reference)
        assertNull(req.payment_notes)
    }

    @Test
    fun `SaleStatusUpdateRequest - mark paid with reference`() {
        val req = SaleStatusUpdateRequest(
            status = "paid",
            payment_reference = "TXN-12345",
            payment_notes = "Paid via bank transfer"
        )
        assertEquals("paid", req.status)
        assertEquals("TXN-12345", req.payment_reference)
        assertEquals("Paid via bank transfer", req.payment_notes)
    }

    // ========== Collection operations mimicking VM logic ==========

    @Test
    fun `getCountByStatus filters correctly`() {
        val sales = listOf(
            createSale("1", "pending"),
            createSale("2", "approved"),
            createSale("3", "pending"),
            createSale("4", "paid"),
            createSale("5", "pending")
        )

        val pendingCount = sales.count { it.commission_status == "pending" }
        val approvedCount = sales.count { it.commission_status == "approved" }
        val paidCount = sales.count { it.commission_status == "paid" }

        assertEquals(3, pendingCount)
        assertEquals(1, approvedCount)
        assertEquals(1, paidCount)
    }

    @Test
    fun `getTotalCommission sums all commissions`() {
        val sales = listOf(
            createSale("1", "pending", commission = 10.0),
            createSale("2", "approved", commission = 20.0),
            createSale("3", "paid", commission = 30.0)
        )

        val total = sales.sumOf { it.commission_amount }
        assertEquals(60.0, total, 0.001)
    }

    @Test
    fun `getPendingCommission sums only pending`() {
        val sales = listOf(
            createSale("1", "pending", commission = 10.0),
            createSale("2", "approved", commission = 20.0),
            createSale("3", "pending", commission = 15.0)
        )

        val pendingTotal = sales.filter { it.commission_status == "pending" }
            .sumOf { it.commission_amount }
        assertEquals(25.0, pendingTotal, 0.001)
    }

    @Test
    fun `getTotalCommission empty list returns zero`() {
        val sales = emptyList<AdminAffiliateSaleResponse>()
        assertEquals(0.0, sales.sumOf { it.commission_amount }, 0.001)
    }

    // ========== Category list operations mimicking VM logic ==========

    @Test
    fun `category active count`() {
        val categories = listOf(
            createCategory("1", "A", active = true),
            createCategory("2", "B", active = false),
            createCategory("3", "C", active = true)
        )
        assertEquals(2, categories.count { it.is_active })
        assertEquals(1, categories.count { !it.is_active })
    }

    @Test
    fun `category search filter by name`() {
        val categories = listOf(
            createCategory("1", "Electronics", active = true),
            createCategory("2", "Fashion", active = true),
            createCategory("3", "Electronic Gadgets", active = true)
        )
        val query = "electro"
        val filtered = categories.filter {
            it.name.contains(query, ignoreCase = true)
        }
        assertEquals(2, filtered.size)
    }

    // ========== Helpers ==========

    private fun createSale(
        id: String,
        status: String,
        commission: Double = 5.0
    ) = AdminAffiliateSaleResponse(
        id = id,
        order_id = "ord-$id",
        product_id = "prod-$id",
        promotion_id = null,
        buyer_user_id = "buyer-$id",
        promoter_user_id = "promoter-$id",
        sale_amount = 100.0,
        product_price = 100.0,
        quantity = 1,
        commission_rate = 10.0,
        commission_amount = commission,
        commission_status = status,
        paid_at = null,
        payment_reference = null,
        created_at = "2025-01-01"
    )

    private fun createCategory(
        id: String,
        name: String,
        active: Boolean
    ) = AdminCategoryResponse(
        id = id,
        name = name,
        name_ar = null,
        slug = name.lowercase(),
        icon_url = null,
        parent_id = null,
        display_order = 0,
        is_active = active,
        created_at = "2025-01-01"
    )
}
