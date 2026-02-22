package com.project.e_commerce.data.repository

import com.project.e_commerce.data.mappers.toProduct
import com.project.e_commerce.data.remote.api.MarketplaceApiService
import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Category
import com.project.e_commerce.domain.model.Product
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.ProductRepository

/**
 * Implémentation du ProductRepository utilisant l'API Marketplace du backend.
 * 
 * Migré de Firebase Firestore vers le backend API (Phase 1).
 */
class ProductRepositoryImpl(
    private val marketplaceApi: MarketplaceApiService
) : ProductRepository {
    
    companion object {
        /** Default page size for product listings. Keeps network payload reasonable. */
        private const val DEFAULT_PAGE_SIZE = 30
    }
    
    override suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val response = marketplaceApi.getProducts(
                page = 1,
                limit = DEFAULT_PAGE_SIZE,
                sortBy = "relevance"
            )
            val products = response.items.map { it.toProduct() }
            Result.Success(products)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun getProductsByCategory(categoryId: String): Result<List<Product>> {
        return try {
            val response = marketplaceApi.getProducts(
                categoryId = categoryId,
                page = 1,
                limit = DEFAULT_PAGE_SIZE
            )
            val products = response.items.map { it.toProduct() }
            Result.Success(products)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }

    override suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val marketplaceProduct = marketplaceApi.getProduct(productId)
            Result.Success(marketplaceProduct.toProduct())
        } catch (e: Exception) {
            // Check if 404
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val response = marketplaceApi.getProducts(
                search = query,
                page = 1,
                limit = DEFAULT_PAGE_SIZE
            )
            val products = response.items.map { it.toProduct() }
            Result.Success(products)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }

    override suspend fun getProductsByUser(userId: String): Result<List<Product>> {
        // Les produits du marketplace appartiennent à l'admin/système, pas aux users standards
        // On pourrait retourner les produits promus par l'user à l'avenir
        return Result.Success(emptyList()) 
    }
    
    override suspend fun createProduct(product: Product): Result<Product> {
        return Result.Error(ApiError.Unknown("Create product not supported in client app. Use Admin Panel."))
    }
    
    override suspend fun updateProduct(product: Product): Result<Unit> {
        return Result.Error(ApiError.Unknown("Update product not supported in client app. Use Admin Panel."))
    }
    
    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return Result.Error(ApiError.Unknown("Delete product not supported in client app. Use Admin Panel."))
    }
    
    override suspend fun getAllCategories(): Result<List<Category>> {
        return try {
            val categoriesDtos = marketplaceApi.getCategories()
            val categories = categoriesDtos.map { dto ->
                Category(
                    id = dto.id,
                    name = dto.name,
                    image = dto.iconUrl ?: "",
                    // Add other fields mappings if Category model has changed, assuming minimal Category model for now
                )
            }
            Result.Success(categories)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun getCategoryById(categoryId: String): Result<Category> {
        // Optimisation: fetch all and filter, or add endpoint if needed.
        // For now, re-use getAllCategories or fail?
        // MarketplaceApi doesn't have getCategoryById explicitly, only getCategories(parentId)
        // Let's rely on getAllCategories for now or mock it since it's rarely used individually
        return try {
            val categories = marketplaceApi.getCategories()
            val category = categories.find { it.id == categoryId }
            if (category != null) {
                Result.Success(Category(category.id, category.name, category.iconUrl ?: ""))
            } else {
                Result.Error(ApiError.NotFound)
            }
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun getPopularProducts(limit: Int): Result<List<Product>> {
        return try {
            val response = marketplaceApi.getProducts(
                sortBy = "sales", // or 'rating'
                limit = limit
            )
            val products = response.items.map { it.toProduct() }
            Result.Success(products)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun getRecentProducts(limit: Int): Result<List<Product>> {
        return try {
            // Sort by newest (created_at DESC) via the marketplace API
            val response = marketplaceApi.getProducts(
                sortBy = "recent",
                limit = limit
            )
            val products = response.items.map { it.toProduct() }
            Result.Success(products)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
}
