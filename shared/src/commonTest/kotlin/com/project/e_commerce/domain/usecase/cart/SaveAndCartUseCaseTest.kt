package com.project.e_commerce.domain.usecase.cart

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Cart
import com.project.e_commerce.domain.model.CartItem
import com.project.e_commerce.domain.model.Post
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CartRepository
import com.project.e_commerce.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [SaveAndCartUseCase].
 * Covers plan Section 3.1 — P1 "SaveAndCartUseCase".
 *
 * Uses hand-written fakes (no mockk) — compatible with KMP commonTest.
 */

// ── Fakes ────────────────────────────────────────────────────────────────

private class FakeCartRepository(
    private val addToCartResult: Result<Unit> = Result.Success(Unit)
) : CartRepository {

    var addToCartCalled = false
    var lastUserId: String? = null
    var lastItem: CartItem? = null

    override suspend fun addToCart(userId: String, item: CartItem): Result<Unit> {
        addToCartCalled = true
        lastUserId = userId
        lastItem = item
        return addToCartResult
    }

    override suspend fun getUserCart(userId: String): Result<Cart> = Result.Success(Cart())
    override fun getUserCartFlow(userId: String): Flow<Cart?> = flowOf(null)
    override suspend fun removeFromCart(userId: String, lineId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun updateQuantity(userId: String, lineId: String, quantity: Int): Result<Unit> = Result.Success(Unit)
    override suspend fun clearCart(userId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun syncCart(userId: String): Result<Cart> = Result.Success(Cart())
}

private class FakePostRepository(
    private val shouldThrow: Boolean = false
) : PostRepository {

    var bookmarkCalled = false
    var lastPostId: String? = null
    var lastUserId: String? = null

    override suspend fun bookmarkPost(postId: String, userId: String) {
        bookmarkCalled = true
        lastPostId = postId
        lastUserId = userId
        if (shouldThrow) throw RuntimeException("Fake network error")
    }

    override suspend fun createPost(type: String, mediaUrl: String, caption: String?): Post = Post()
    override suspend fun likePost(postId: String, userId: String) {}
    override suspend fun unlikePost(postId: String, userId: String) {}
    override suspend fun unbookmarkPost(postId: String, userId: String) {}
    override suspend fun deletePost(postId: String, userId: String) {}
    override suspend fun getLikedPosts(userId: String): List<Post> = emptyList()
    override suspend fun getBookmarkedPosts(userId: String): List<Post> = emptyList()
    override suspend fun checkPostLikeStatus(postId: String, userId: String): Boolean = false
    override suspend fun checkPostBookmarkStatus(postId: String, userId: String): Boolean = false
    override suspend fun getOrCreatePostForMarketplaceProduct(productUuid: String): Post = Post()
}

// ── Helpers ───────────────────────────────────────────────────────────────

private fun cartItem(productId: String = "prod1", quantity: Int = 1) = CartItem(
    productId = productId,
    productName = "Widget",
    price = 9.99,
    quantity = quantity
)

private fun makeUseCase(
    cartRepo: CartRepository,
    postRepo: PostRepository
): SaveAndCartUseCase {
    val addToCart = AddToCartUseCase(cartRepo)
    val bookmarkPost = BookmarkPostUseCase(postRepo)
    return SaveAndCartUseCase(addToCart, bookmarkPost)
}

// ── Tests ─────────────────────────────────────────────────────────────────

class SaveAndCartUseCaseTest {

    // ─── P1: happy path ──────────────────────────────────────────────────

    @Test
    fun `both cart add and bookmark succeed returns Success`() = runTest {
        val cartRepo = FakeCartRepository(addToCartResult = Result.Success(Unit))
        val postRepo = FakePostRepository()
        val useCase = makeUseCase(cartRepo, postRepo)

        val result = useCase("user1", "post1", cartItem())

        assertTrue(result is Result.Success)
        assertTrue(cartRepo.addToCartCalled, "addToCart must have been called")
        assertTrue(postRepo.bookmarkCalled, "bookmarkPost must have been called")
    }

    // ─── P0: cart failure propagates ─────────────────────────────────────

    @Test
    fun `cart add failure returns Error without calling bookmark`() = runTest {
        val error = ApiError.NetworkError("timeout")
        val cartRepo = FakeCartRepository(addToCartResult = Result.Error(error))
        val postRepo = FakePostRepository()
        val useCase = makeUseCase(cartRepo, postRepo)

        val result = useCase("user1", "post1", cartItem())

        assertTrue(result is Result.Error, "Must propagate cart error")
        assertFalse(postRepo.bookmarkCalled, "bookmarkPost must NOT be called after cart failure")
    }

    // ─── P1: bookmark failure swallowed ──────────────────────────────────

    @Test
    fun `bookmark throwing exception still returns Success`() = runTest {
        val cartRepo = FakeCartRepository(addToCartResult = Result.Success(Unit))
        val postRepo = FakePostRepository(shouldThrow = true)
        val useCase = makeUseCase(cartRepo, postRepo)

        // Must NOT throw; bookmark error is silently swallowed
        val result = useCase("user1", "post1", cartItem())

        assertTrue(result is Result.Success, "Bookmark failure must not propagate")
    }

    // ─── P0: blank postId skips bookmark ─────────────────────────────────

    @Test
    fun `blank postId skips bookmark but cart still succeeds`() = runTest {
        val cartRepo = FakeCartRepository(addToCartResult = Result.Success(Unit))
        val postRepo = FakePostRepository()
        val useCase = makeUseCase(cartRepo, postRepo)

        val result = useCase("user1", "", cartItem())  // empty postId

        assertTrue(result is Result.Success)
        assertFalse(postRepo.bookmarkCalled, "bookmarkPost must NOT be called for blank postId")
    }

    // ─── P0: input validation — blank userId ─────────────────────────────

    @Test
    fun `blank userId returns Error before hitting repository`() = runTest {
        val cartRepo = FakeCartRepository()
        val postRepo = FakePostRepository()
        val useCase = makeUseCase(cartRepo, postRepo)

        val result = useCase("", "post1", cartItem())

        assertTrue(result is Result.Error)
        assertFalse(cartRepo.addToCartCalled, "addToCart must NOT be called for blank userId")
    }

    // ─── P0: input validation — blank productId ──────────────────────────

    @Test
    fun `blank productId returns Error via cart validation`() = runTest {
        val cartRepo = FakeCartRepository()
        val postRepo = FakePostRepository()
        val useCase = makeUseCase(cartRepo, postRepo)

        val result = useCase("user1", "post1", cartItem(productId = ""))

        assertTrue(result is Result.Error)
    }

    // ─── P1: cart receives correct userId and item ────────────────────────

    @Test
    fun `correct userId and item forwarded to cart repository`() = runTest {
        val cartRepo = FakeCartRepository()
        val postRepo = FakePostRepository()
        val useCase = makeUseCase(cartRepo, postRepo)
        val item = cartItem(productId = "abc", quantity = 3)

        useCase("alice", "post1", item)

        assertEquals("alice", cartRepo.lastUserId)
        assertEquals(item.productId, cartRepo.lastItem?.productId)
        assertEquals(3, cartRepo.lastItem?.quantity)
    }

    // ─── P1: correct postId and userId forwarded to bookmark ─────────────

    @Test
    fun `correct postId and userId forwarded to bookmark use case`() = runTest {
        val cartRepo = FakeCartRepository()
        val postRepo = FakePostRepository()
        val useCase = makeUseCase(cartRepo, postRepo)

        useCase("alice", "reel99", cartItem())

        assertEquals("reel99", postRepo.lastPostId)
        assertEquals("alice", postRepo.lastUserId)
    }

    // ─── P1: quantity=0 triggers cart validation error ────────────────────

    @Test
    fun `quantity zero returns validation Error`() = runTest {
        val cartRepo = FakeCartRepository()
        val postRepo = FakePostRepository()
        val useCase = makeUseCase(cartRepo, postRepo)

        val result = useCase("user1", "post1", cartItem(quantity = 0))

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is ApiError.ValidationError,
            "Expected ValidationError for quantity=0, got $error")
    }
}
