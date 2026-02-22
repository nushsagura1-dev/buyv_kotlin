package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.PostApiService
import com.project.e_commerce.data.remote.mapper.toProduct
import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Category
import com.project.e_commerce.domain.model.Product
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.ProductRepository
import io.ktor.client.plugins.*
import io.ktor.utils.io.errors.*

/**
 * Network implementation of ProductRepository using FastAPI backend
 * Uses Post API since products are represented as posts in the backend
 */
class ProductNetworkRepository(
    private val postApi: PostApiService
) : ProductRepository {
    
    override suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val posts = postApi.getFeed(skip = 0, limit = 100)
            // Only convert reel-type posts to Products for the reels feed
            // Shadow product posts (type="product") should NOT appear as reels
            val products = posts
                .filter { it.type == "reel" }
                .map { it.toProduct() }
            Result.Success(products)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val post = postApi.getPostById(productId)
            Result.Success(post.toProduct())
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getProductsByCategory(categoryId: String): Result<List<Product>> {
        return try {
            // Search by category - backend may need category tag support
            val posts = postApi.searchPosts(
                query = categoryId,
                postType = "PRODUCT",
                limit = 100
            )
            val products = posts.map { it.toProduct() }
            Result.Success(products)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val posts = postApi.searchPosts(
                query = query,
                postType = "PRODUCT",
                limit = 100
            )
            val products = posts.map { it.toProduct() }
            Result.Success(products)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getProductsByUser(userId: String): Result<List<Product>> {
        return try {
            val posts = postApi.getPostsByUser(userId = userId, limit = 100)
            val products = posts.map { it.toProduct() }
            Result.Success(products)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun createProduct(product: Product): Result<Product> {
        // TODO: Implémenter quand l'endpoint backend sera disponible
        return Result.Error(ApiError.Unknown("Create product not yet implemented"))
    }
    
    override suspend fun updateProduct(product: Product): Result<Unit> {
        // TODO: Implémenter quand l'endpoint backend sera disponible
        return Result.Error(ApiError.Unknown("Update product not yet implemented"))
    }
    
    override suspend fun deleteProduct(productId: String): Result<Unit> {
        // TODO: Implémenter quand l'endpoint backend sera disponible
        return Result.Error(ApiError.Unknown("Delete product not yet implemented"))
    }
    
    override suspend fun getAllCategories(): Result<List<Category>> {
        // TODO: Implémenter quand l'endpoint backend sera disponible
        return Result.Error(ApiError.Unknown("Get all categories not yet implemented"))
    }
    
    override suspend fun getCategoryById(categoryId: String): Result<Category> {
        // TODO: Implémenter quand l'endpoint backend sera disponible
        return Result.Error(ApiError.Unknown("Get category by id not yet implemented"))
    }
    
    override suspend fun getPopularProducts(limit: Int): Result<List<Product>> {
        return try {
            // Get all products and sort by likes (popularity)
            val posts = postApi.getFeed(skip = 0, limit = limit * 2)
            val products = posts
                .sortedByDescending { it.likesCount }
                .take(limit)
                .map { it.toProduct() }
            Result.Success(products)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getRecentProducts(limit: Int): Result<List<Product>> {
        return try {
            val posts = postApi.getFeed(skip = 0, limit = limit * 2)
            val products = posts
                .take(limit)
                .map { it.toProduct() }
            Result.Success(products)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
}
