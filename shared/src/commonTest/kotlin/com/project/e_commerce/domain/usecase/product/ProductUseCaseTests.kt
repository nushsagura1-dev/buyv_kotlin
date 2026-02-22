package com.project.e_commerce.domain.usecase.product

import com.project.e_commerce.domain.model.*
import com.project.e_commerce.domain.repository.ProductRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UseCase tests for product domain: GetProductDetails, GetProducts, GetCategories.
 *
 * Uses hand-written FakeProductRepository.
 */

// ── Fake Repository ────────────────────────────────────────
private class FakeProductRepository(
    var allProductsResult: Result<List<Product>> = Result.Success(emptyList()),
    var byCategoryResult: Result<List<Product>> = Result.Success(emptyList()),
    var byIdResult: Result<Product> = Result.Success(Product(id = "p1")),
    var searchResult: Result<List<Product>> = Result.Success(emptyList()),
    var byUserResult: Result<List<Product>> = Result.Success(emptyList()),
    var createResult: Result<Product> = Result.Success(Product(id = "p1")),
    var updateResult: Result<Unit> = Result.Success(Unit),
    var deleteResult: Result<Unit> = Result.Success(Unit),
    var categoriesResult: Result<List<Category>> = Result.Success(emptyList()),
    var categoryByIdResult: Result<Category> = Result.Success(Category(id = "c1")),
    var popularResult: Result<List<Product>> = Result.Success(emptyList()),
    var recentResult: Result<List<Product>> = Result.Success(emptyList()),
) : ProductRepository {

    override suspend fun getAllProducts(): Result<List<Product>> = allProductsResult
    override suspend fun getProductsByCategory(categoryId: String): Result<List<Product>> = byCategoryResult
    override suspend fun getProductById(productId: String): Result<Product> = byIdResult
    override suspend fun searchProducts(query: String): Result<List<Product>> = searchResult
    override suspend fun getProductsByUser(userId: String): Result<List<Product>> = byUserResult
    override suspend fun createProduct(product: Product): Result<Product> = createResult
    override suspend fun updateProduct(product: Product): Result<Unit> = updateResult
    override suspend fun deleteProduct(productId: String): Result<Unit> = deleteResult
    override suspend fun getAllCategories(): Result<List<Category>> = categoriesResult
    override suspend fun getCategoryById(categoryId: String): Result<Category> = categoryByIdResult
    override suspend fun getPopularProducts(limit: Int): Result<List<Product>> = popularResult
    override suspend fun getRecentProducts(limit: Int): Result<List<Product>> = recentResult
}


// ════════════════════════════════════════════════
// GET PRODUCT DETAILS USE CASE
// ════════════════════════════════════════════════

class GetProductDetailsUseCaseTest {

    @Test
    fun get_product_success() = runTest {
        val product = Product(id = "p1", name = "Test Product", price = "29.99")
        val repo = FakeProductRepository(byIdResult = Result.Success(product))
        val result = GetProductDetailsUseCase(repo)("p1")

        assertIs<Result.Success<Product>>(result)
        assertEquals("p1", result.data.id)
        assertEquals("Test Product", result.data.name)
    }

    @Test
    fun get_product_blank_id_returns_validation_error() = runTest {
        val repo = FakeProductRepository()
        val result = GetProductDetailsUseCase(repo)("")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
        assertTrue(result.error.message.contains("Product ID", ignoreCase = true))
    }

    @Test
    fun get_product_whitespace_id_returns_validation_error() = runTest {
        val repo = FakeProductRepository()
        val result = GetProductDetailsUseCase(repo)("   ")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
    }

    @Test
    fun get_product_not_found() = runTest {
        val repo = FakeProductRepository(byIdResult = Result.Error(ApiError.NotFound))
        val result = GetProductDetailsUseCase(repo)("missing")

        assertIs<Result.Error>(result)
        assertIs<ApiError.NotFound>(result.error)
    }

    @Test
    fun get_product_network_error() = runTest {
        val repo = FakeProductRepository(byIdResult = Result.Error(ApiError.NetworkError))
        val result = GetProductDetailsUseCase(repo)("p1")

        assertIs<Result.Error>(result)
        assertIs<ApiError.NetworkError>(result.error)
    }
}


// ════════════════════════════════════════════════
// GET PRODUCTS USE CASE
// ════════════════════════════════════════════════

class GetProductsUseCaseTest {

    @Test
    fun get_all_products_success() = runTest {
        val products = listOf(
            Product(id = "p1", name = "A"),
            Product(id = "p2", name = "B"),
        )
        val repo = FakeProductRepository(allProductsResult = Result.Success(products))
        val result = GetProductsUseCase(repo).getAllProducts()

        assertIs<Result.Success<List<Product>>>(result)
        assertEquals(2, result.data.size)
    }

    @Test
    fun invoke_returns_all_products() = runTest {
        val products = listOf(Product(id = "p1"))
        val repo = FakeProductRepository(allProductsResult = Result.Success(products))
        val result = GetProductsUseCase(repo)()

        assertIs<Result.Success<List<Product>>>(result)
        assertEquals(1, result.data.size)
    }

    @Test
    fun get_by_category_success() = runTest {
        val products = listOf(Product(id = "p3", categoryId = "c1"))
        val repo = FakeProductRepository(byCategoryResult = Result.Success(products))
        val result = GetProductsUseCase(repo).getProductsByCategory("c1")

        assertIs<Result.Success<List<Product>>>(result)
        assertEquals("c1", result.data.first().categoryId)
    }

    @Test
    fun get_popular_products() = runTest {
        val products = listOf(Product(id = "pop1"), Product(id = "pop2"))
        val repo = FakeProductRepository(popularResult = Result.Success(products))
        val result = GetProductsUseCase(repo).getPopularProducts(5)

        assertIs<Result.Success<List<Product>>>(result)
        assertEquals(2, result.data.size)
    }

    @Test
    fun get_recent_products() = runTest {
        val products = listOf(Product(id = "new1"))
        val repo = FakeProductRepository(recentResult = Result.Success(products))
        val result = GetProductsUseCase(repo).getRecentProducts(3)

        assertIs<Result.Success<List<Product>>>(result)
        assertEquals(1, result.data.size)
    }

    @Test
    fun search_with_query() = runTest {
        val products = listOf(Product(id = "s1", name = "Shoes"))
        val repo = FakeProductRepository(searchResult = Result.Success(products))
        val result = GetProductsUseCase(repo).searchProducts("shoes")

        assertIs<Result.Success<List<Product>>>(result)
        assertEquals("Shoes", result.data.first().name)
    }

    @Test
    fun search_blank_query_returns_all_products() = runTest {
        val allProducts = listOf(Product(id = "a1"), Product(id = "a2"))
        val repo = FakeProductRepository(allProductsResult = Result.Success(allProducts))
        val result = GetProductsUseCase(repo).searchProducts("")

        assertIs<Result.Success<List<Product>>>(result)
        assertEquals(2, result.data.size)
    }

    @Test
    fun get_best_sellers_delegates_to_popular() = runTest {
        val products = listOf(Product(id = "bs1"))
        val repo = FakeProductRepository(popularResult = Result.Success(products))
        val result = GetProductsUseCase(repo).getBestSellers()

        assertIs<Result.Success<List<Product>>>(result)
        assertEquals(1, result.data.size)
    }

    @Test
    fun server_error_propagates() = runTest {
        val repo = FakeProductRepository(allProductsResult = Result.Error(ApiError.ServerError))
        val result = GetProductsUseCase(repo)()

        assertIs<Result.Error>(result)
        assertIs<ApiError.ServerError>(result.error)
    }
}


// ════════════════════════════════════════════════
// GET CATEGORIES USE CASE
// ════════════════════════════════════════════════

class GetCategoriesUseCaseTest {

    @Test
    fun get_categories_success() = runTest {
        val categories = listOf(
            Category(id = "c1", name = "Electronics"),
            Category(id = "c2", name = "Clothing"),
        )
        val repo = FakeProductRepository(categoriesResult = Result.Success(categories))
        val result = GetCategoriesUseCase(repo)()

        assertIs<Result.Success<List<Category>>>(result)
        assertEquals(2, result.data.size)
        assertEquals("Electronics", result.data.first().name)
    }

    @Test
    fun get_categories_via_getAllCategories() = runTest {
        val categories = listOf(Category(id = "c1", name = "Books"))
        val repo = FakeProductRepository(categoriesResult = Result.Success(categories))
        val result = GetCategoriesUseCase(repo).getAllCategories()

        assertIs<Result.Success<List<Category>>>(result)
        assertEquals("Books", result.data.first().name)
    }

    @Test
    fun get_categories_empty_list() = runTest {
        val repo = FakeProductRepository(categoriesResult = Result.Success(emptyList()))
        val result = GetCategoriesUseCase(repo)()

        assertIs<Result.Success<List<Category>>>(result)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun get_categories_error() = runTest {
        val repo = FakeProductRepository(categoriesResult = Result.Error(ApiError.ServerError))
        val result = GetCategoriesUseCase(repo)()

        assertIs<Result.Error>(result)
        assertIs<ApiError.ServerError>(result.error)
    }
}
