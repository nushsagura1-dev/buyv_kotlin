package com.project.e_commerce.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CartTest {

    // ========== CartItem.totalPrice ==========

    @Test
    fun cartItem_totalPrice_single_quantity() {
        val item = CartItem(price = 25.0, quantity = 1)
        assertEquals(25.0, item.totalPrice)
    }

    @Test
    fun cartItem_totalPrice_multiple_quantity() {
        val item = CartItem(price = 10.0, quantity = 3)
        assertEquals(30.0, item.totalPrice)
    }

    @Test
    fun cartItem_totalPrice_zero_quantity() {
        val item = CartItem(price = 10.0, quantity = 0)
        assertEquals(0.0, item.totalPrice)
    }

    @Test
    fun cartItem_totalPrice_zero_price() {
        val item = CartItem(price = 0.0, quantity = 5)
        assertEquals(0.0, item.totalPrice)
    }

    @Test
    fun cartItem_totalPrice_decimal_price() {
        val item = CartItem(price = 9.99, quantity = 2)
        assertEquals(19.98, item.totalPrice, 0.001)
    }

    // ========== Cart.calculateTotal ==========

    @Test
    fun cart_calculateTotal_empty_cart() {
        val cart = Cart(items = emptyList())
        assertEquals(0.0, cart.calculateTotal())
    }

    @Test
    fun cart_calculateTotal_single_item() {
        val cart = Cart(items = listOf(
            CartItem(price = 25.0, quantity = 1)
        ))
        assertEquals(25.0, cart.calculateTotal())
    }

    @Test
    fun cart_calculateTotal_multiple_items() {
        val cart = Cart(items = listOf(
            CartItem(price = 10.0, quantity = 2),
            CartItem(price = 5.0, quantity = 3),
            CartItem(price = 20.0, quantity = 1)
        ))
        // 10*2 + 5*3 + 20*1 = 20 + 15 + 20 = 55
        assertEquals(55.0, cart.calculateTotal())
    }

    @Test
    fun cart_calculateTotal_with_quantities() {
        val cart = Cart(items = listOf(
            CartItem(price = 99.99, quantity = 2)
        ))
        assertEquals(199.98, cart.calculateTotal(), 0.001)
    }

    // ========== Cart.getTotalItemCount ==========

    @Test
    fun cart_getTotalItemCount_empty_cart() {
        val cart = Cart(items = emptyList())
        assertEquals(0, cart.getTotalItemCount())
    }

    @Test
    fun cart_getTotalItemCount_single_item_single_quantity() {
        val cart = Cart(items = listOf(
            CartItem(quantity = 1)
        ))
        assertEquals(1, cart.getTotalItemCount())
    }

    @Test
    fun cart_getTotalItemCount_multiple_items_with_quantities() {
        val cart = Cart(items = listOf(
            CartItem(quantity = 2),
            CartItem(quantity = 3),
            CartItem(quantity = 1)
        ))
        assertEquals(6, cart.getTotalItemCount())
    }

    @Test
    fun cart_getTotalItemCount_counts_quantities_not_distinct_items() {
        val cart = Cart(items = listOf(
            CartItem(productId = "A", quantity = 5),
            CartItem(productId = "B", quantity = 10)
        ))
        // Should be 15, not 2
        assertEquals(15, cart.getTotalItemCount())
    }

    // ========== Cart defaults ==========

    @Test
    fun cart_defaults_empty_items() {
        val cart = Cart()
        assertEquals(emptyList(), cart.items)
        assertEquals(0.0, cart.subtotal)
    }

    @Test
    fun cartItem_defaults() {
        val item = CartItem()
        assertEquals("", item.id)
        assertEquals("", item.productId)
        assertEquals(0.0, item.price)
        assertEquals(1, item.quantity)
        assertEquals(null, item.size)
        assertEquals(null, item.color)
        assertEquals(emptyMap(), item.attributes)
    }

    @Test
    fun cartStats_defaults() {
        val stats = CartStats()
        assertEquals(0, stats.itemCount)
        assertEquals(0.0, stats.subtotal)
        assertEquals(0.0, stats.tax)
        assertEquals(0.0, stats.shipping)
        assertEquals(0.0, stats.total)
    }
}
