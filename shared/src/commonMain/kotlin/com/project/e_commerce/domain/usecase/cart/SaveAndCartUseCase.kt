package com.project.e_commerce.domain.usecase.cart

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.CartItem
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.usecase.post.BookmarkPostUseCase

/**
 * Unified "Save & Cart" use case (SET-004).
 *
 * Combines [AddToCartUseCase] + [BookmarkPostUseCase] into a single atomic action,
 * replacing the redundant separate Save icon in the ReelsView sidebar.
 *
 * Contract:
 * - Cart add is the primary action — its failure propagates as [Result.Error].
 * - Bookmark is a secondary, best-effort action — its failure is silently ignored
 *   so the user always gets the product in their cart even if bookmarking fails.
 */
class SaveAndCartUseCase(
    private val addToCart: AddToCartUseCase,
    private val bookmarkPost: BookmarkPostUseCase
) {

    /**
     * @param userId    Authenticated user UID.
     * @param postId    Post/reel UID to bookmark (can be empty — bookmark silently skipped).
     * @param cartItem  Product item to add to the cart.
     * @return [Result.Success] if cart add succeeded; [Result.Error] otherwise.
     */
    suspend operator fun invoke(
        userId: String,
        postId: String,
        cartItem: CartItem
    ): Result<Unit> {
        // Primary: add to cart — failure stops execution
        val cartResult = addToCart(userId, cartItem)
        if (cartResult is Result.Error) return cartResult

        // Secondary: bookmark post — failure silently swallowed
        if (postId.isNotBlank()) {
            try {
                bookmarkPost(postId, userId)
            } catch (_: Exception) {
                // Non-critical — cart was already added successfully
            }
        }

        return Result.Success(Unit)
    }
}
