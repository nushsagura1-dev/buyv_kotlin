package com.project.e_commerce.domain.model

/**
 * Represents the authentication state of the current session.
 *
 * - [Guest]          : No token present; user can browse content but cannot interact.
 * - [Authenticated]  : Valid token + resolved UserProfile; full access.
 *
 * Used by AuthGuard, MyNavHost startDestination and lazy-login flow (AUTH-003, AUTH-004).
 */
sealed class AuthState {

    /** Anonymous / unauthenticated session — content browsable, interactions blocked. */
    data object Guest : AuthState()

    /** Active authenticated session with a loaded user profile. */
    data class Authenticated(val user: UserProfile) : AuthState()

    /** True for any logged-in state. */
    val isAuthenticated: Boolean get() = this is Authenticated

    /** Convenience accessor — null when Guest. */
    val userOrNull: UserProfile? get() = (this as? Authenticated)?.user
}

/**
 * Actions that require authentication before executing.
 * Stored as [pendingAction] when the user triggers one while in [AuthState.Guest].
 */
sealed class AuthAction {
    data class Like(val postId: String) : AuthAction()
    data class Comment(val postId: String) : AuthAction()
    data class AddToCart(val productId: String) : AuthAction()
    data class Follow(val userId: String) : AuthAction()
    data class Bookmark(val postId: String) : AuthAction()
    data object Upload : AuthAction()
    data object Purchase : AuthAction()
}
