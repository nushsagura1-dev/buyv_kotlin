package com.project.e_commerce.domain.model.marketplace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketplaceModelsTest {

    // Helper to create a MarketplaceProduct with defaults
    private fun product(
        originalPrice: Double = 100.0,
        sellingPrice: Double = 80.0,
        commissionRate: Double = 10.0,
        commissionAmount: Double? = null,
        commissionType: String = "percentage",
        status: String = "active"
    ) = MarketplaceProduct(
        id = "1",
        name = "Test Product",
        description = "A test product",
        originalPrice = originalPrice,
        sellingPrice = sellingPrice,
        commissionRate = commissionRate,
        commissionAmount = commissionAmount,
        commissionType = commissionType,
        status = status,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    // ========== getDiscountPercentage ==========

    @Test
    fun getDiscountPercentage_with_discount() {
        val p = product(originalPrice = 100.0, sellingPrice = 80.0)
        assertEquals(20, p.getDiscountPercentage())
    }

    @Test
    fun getDiscountPercentage_50_percent() {
        val p = product(originalPrice = 200.0, sellingPrice = 100.0)
        assertEquals(50, p.getDiscountPercentage())
    }

    @Test
    fun getDiscountPercentage_no_discount_returns_null() {
        val p = product(originalPrice = 100.0, sellingPrice = 100.0)
        assertNull(p.getDiscountPercentage())
    }

    @Test
    fun getDiscountPercentage_selling_higher_returns_null() {
        val p = product(originalPrice = 80.0, sellingPrice = 100.0)
        assertNull(p.getDiscountPercentage())
    }

    @Test
    fun getDiscountPercentage_rounds_down() {
        // 33.33...% discount → should return 33
        val p = product(originalPrice = 150.0, sellingPrice = 100.0)
        assertEquals(33, p.getDiscountPercentage())
    }

    // ========== isAvailable ==========

    @Test
    fun isAvailable_active_true() {
        assertTrue(product(status = "active").isAvailable())
    }

    @Test
    fun isAvailable_inactive_false() {
        assertFalse(product(status = "inactive").isAvailable())
    }

    @Test
    fun isAvailable_outOfStock_false() {
        assertFalse(product(status = "out_of_stock").isAvailable())
    }

    // ========== getEstimatedCommission ==========

    @Test
    fun getEstimatedCommission_percentage_type() {
        val p = product(sellingPrice = 100.0, commissionRate = 10.0, commissionType = "percentage")
        // Formula: sellingPrice * (1 - (1 / (1 + (commissionRate / 100))))
        // = 100 * (1 - (1 / 1.1)) = 100 * (1 - 0.9090...) = 100 * 0.0909... ≈ 9.09
        val expected = 100.0 * (1.0 - (1.0 / (1.0 + (10.0 / 100.0))))
        assertEquals(expected, p.getEstimatedCommission(), 0.01)
    }

    @Test
    fun getEstimatedCommission_fixed_type_with_amount() {
        val p = product(commissionType = "fixed", commissionAmount = 15.0)
        assertEquals(15.0, p.getEstimatedCommission())
    }

    @Test
    fun getEstimatedCommission_fixed_type_no_amount() {
        val p = product(commissionType = "fixed", commissionAmount = null)
        assertEquals(0.0, p.getEstimatedCommission())
    }

    @Test
    fun getEstimatedCommission_zero_rate() {
        val p = product(sellingPrice = 100.0, commissionRate = 0.0, commissionType = "percentage")
        assertEquals(0.0, p.getEstimatedCommission(), 0.01)
    }

    // ========== PromoterWallet ==========

    private fun wallet(
        availableAmount: Double = 100.0,
        pendingAmount: Double = 50.0,
        totalEarned: Double = 200.0
    ) = PromoterWallet(
        id = "w1",
        userId = "u1",
        availableAmount = availableAmount,
        pendingAmount = pendingAmount,
        totalEarned = totalEarned,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    @Test
    fun canWithdraw_positive_available_true() {
        assertTrue(wallet(availableAmount = 50.0).canWithdraw())
    }

    @Test
    fun canWithdraw_zero_available_false() {
        assertFalse(wallet(availableAmount = 0.0).canWithdraw())
    }

    @Test
    fun canWithdraw_negative_available_false() {
        assertFalse(wallet(availableAmount = -10.0).canWithdraw())
    }

    // ========== WithdrawalRequest status helpers ==========

    private fun withdrawal(status: String) = WithdrawalRequest(
        id = "wr1",
        walletId = "w1",
        userId = "u1",
        amount = 50.0,
        status = status,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    @Test
    fun withdrawalRequest_isPending() {
        assertTrue(withdrawal("pending").isPending())
        assertFalse(withdrawal("completed").isPending())
    }

    @Test
    fun withdrawalRequest_isCompleted() {
        assertTrue(withdrawal("completed").isCompleted())
        assertFalse(withdrawal("pending").isCompleted())
    }

    @Test
    fun withdrawalRequest_isRejected() {
        assertTrue(withdrawal("rejected").isRejected())
        assertFalse(withdrawal("pending").isRejected())
    }

    // ========== AffiliateSale ==========

    @Test
    fun affiliateSale_defaults() {
        val sale = AffiliateSale(
            id = "s1",
            orderId = "o1",
            productId = "p1",
            promoterUserId = "u1",
            buyerUserId = "u2",
            quantity = 2,
            unitPrice = 25.0,
            saleAmount = 50.0,
            commissionAmount = 5.0,
            commissionStatus = "pending",
            createdAt = "2025-01-01T00:00:00Z"
        )
        assertEquals("USD", sale.currency)
        assertNull(sale.paymentMethod)
        assertNull(sale.paidAt)
    }

    // ========== WalletTransaction.getFormattedAmount ==========

    @Test
    fun walletTransaction_positive_amount_has_plus_sign() {
        val tx = WalletTransaction(
            id = "t1",
            walletId = "w1",
            type = "commission",
            amount = 10.0,
            balanceAfter = 110.0,
            createdAt = "2025-01-01T00:00:00Z"
        )
        assertTrue(tx.getFormattedAmount().startsWith("+"))
    }

    @Test
    fun walletTransaction_negative_amount_has_minus_sign() {
        val tx = WalletTransaction(
            id = "t1",
            walletId = "w1",
            type = "withdrawal",
            amount = -50.0,
            balanceAfter = 60.0,
            createdAt = "2025-01-01T00:00:00Z"
        )
        assertTrue(tx.getFormattedAmount().contains("-"))
    }

    // ========== ProductSortBy ==========

    @Test
    fun productSortBy_values() {
        assertEquals("relevance", ProductSortBy.RELEVANCE.value)
        assertEquals("price_asc", ProductSortBy.PRICE_LOW_TO_HIGH.value)
        assertEquals("price_desc", ProductSortBy.PRICE_HIGH_TO_LOW.value)
        assertEquals("commission_desc", ProductSortBy.COMMISSION.value)
        assertEquals("recent", ProductSortBy.NEWEST.value)
        assertEquals("popular", ProductSortBy.POPULAR.value)
    }

    // ========== ProductCategory defaults ==========

    @Test
    fun productCategory_defaults() {
        val cat = ProductCategory(
            id = "c1",
            name = "Electronics",
            slug = "electronics",
            createdAt = "2025-01-01T00:00:00Z"
        )
        assertNull(cat.nameAr)
        assertNull(cat.iconUrl)
        assertNull(cat.parentId)
        assertEquals(0, cat.displayOrder)
        assertTrue(cat.isActive)
    }
}
