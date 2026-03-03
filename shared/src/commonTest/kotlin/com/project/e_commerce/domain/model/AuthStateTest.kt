package com.project.e_commerce.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [AuthState] sealed class and [AuthAction] sealed class.
 * Covers plan Section 3.1 — P0 "AuthGuard logic".
 */
class AuthStateTest {

    // ── AuthState.Guest ──────────────────────────────────────────────────

    @Test
    fun `Guest state isAuthenticated returns false`() {
        val state: AuthState = AuthState.Guest
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `Guest state userOrNull returns null`() {
        val state: AuthState = AuthState.Guest
        assertNull(state.userOrNull)
    }

    @Test
    fun `Guest is equal to other Guest instance`() {
        assertEquals(AuthState.Guest, AuthState.Guest)
    }

    // ── AuthState.Authenticated ──────────────────────────────────────────

    @Test
    fun `Authenticated state isAuthenticated returns true`() {
        val user = UserProfile(uid = "u1", email = "a@b.com")
        val state: AuthState = AuthState.Authenticated(user)
        assertTrue(state.isAuthenticated)
    }

    @Test
    fun `Authenticated state userOrNull returns the user`() {
        val user = UserProfile(uid = "u1", email = "a@b.com", displayName = "Alice")
        val state: AuthState = AuthState.Authenticated(user)
        assertEquals(user, state.userOrNull)
    }

    @Test
    fun `Authenticated preserves all user fields`() {
        val user = UserProfile(
            uid = "u42",
            email = "seller@buyv.com",
            displayName = "Bob",
            username = "bob42",
            role = "promoter",
            followersCount = 1200
        )
        val state = AuthState.Authenticated(user)
        val retrieved = state.userOrNull!!
        assertEquals("u42", retrieved.uid)
        assertEquals("seller@buyv.com", retrieved.email)
        assertEquals("promoter", retrieved.role)
        assertEquals(1200, retrieved.followersCount)
    }

    // ── Equality / identity ──────────────────────────────────────────────

    @Test
    fun `Two Authenticated states with same user are equal`() {
        val user = UserProfile(uid = "u1")
        assertEquals(AuthState.Authenticated(user), AuthState.Authenticated(user))
    }

    @Test
    fun `Guest is not equal to Authenticated`() {
        val guest = AuthState.Guest
        val auth = AuthState.Authenticated(UserProfile(uid = "x"))
        assertTrue(guest != auth)
    }

    // ── AuthAction sealed class ──────────────────────────────────────────

    @Test
    fun `AuthAction Like stores postId`() {
        val action = AuthAction.Like(postId = "p1")
        assertEquals("p1", action.postId)
    }

    @Test
    fun `AuthAction Comment stores postId`() {
        val action = AuthAction.Comment(postId = "c1")
        assertEquals("c1", action.postId)
    }

    @Test
    fun `AuthAction AddToCart stores productId`() {
        val action = AuthAction.AddToCart(productId = "prod123")
        assertEquals("prod123", action.productId)
    }

    @Test
    fun `AuthAction Follow stores userId`() {
        val action = AuthAction.Follow(userId = "user99")
        assertEquals("user99", action.userId)
    }

    @Test
    fun `AuthAction Bookmark stores postId`() {
        val action = AuthAction.Bookmark(postId = "b1")
        assertEquals("b1", action.postId)
    }

    @Test
    fun `AuthAction Upload is a singleton object`() {
        val a = AuthAction.Upload
        val b = AuthAction.Upload
        assertEquals(a, b)
    }

    @Test
    fun `AuthAction Purchase is a singleton object`() {
        val a = AuthAction.Purchase
        val b = AuthAction.Purchase
        assertEquals(a, b)
    }

    // ── Smart-cast / when exhaustiveness ────────────────────────────────

    @Test
    fun `when on AuthState covers all branches`() {
        val states: List<AuthState> = listOf(
            AuthState.Guest,
            AuthState.Authenticated(UserProfile(uid = "u1"))
        )
        for (state in states) {
            val label: String = when (state) {
                is AuthState.Guest -> "guest"
                is AuthState.Authenticated -> "authenticated"
            }
            assertTrue(label.isNotEmpty())
        }
    }

    @Test
    fun `when on AuthAction covers all branches`() {
        val actions: List<AuthAction> = listOf(
            AuthAction.Like("p1"),
            AuthAction.Comment("p2"),
            AuthAction.AddToCart("prod1"),
            AuthAction.Follow("u1"),
            AuthAction.Bookmark("b1"),
            AuthAction.Upload,
            AuthAction.Purchase
        )
        for (action in actions) {
            val label: String = when (action) {
                is AuthAction.Like -> "like"
                is AuthAction.Comment -> "comment"
                is AuthAction.AddToCart -> "cart"
                is AuthAction.Follow -> "follow"
                is AuthAction.Bookmark -> "bookmark"
                AuthAction.Upload -> "upload"
                AuthAction.Purchase -> "purchase"
            }
            assertTrue(label.isNotEmpty())
        }
    }
}
