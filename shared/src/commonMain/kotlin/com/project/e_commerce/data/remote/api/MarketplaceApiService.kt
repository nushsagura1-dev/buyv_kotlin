package com.project.e_commerce.data.remote.api

import com.project.e_commerce.domain.model.marketplace.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Marketplace API Service
 * Gère tous les endpoints du marketplace
 */
class MarketplaceApiService(private val httpClient: HttpClient) {
    
    // ============================================
    // PRODUCTS
    // ============================================
    
    /**
     * Liste des produits avec filtres et pagination
     * GET /api/v1/marketplace/products
     */
    suspend fun getProducts(
        categoryId: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minCommission: Double? = null,
        search: String? = null,
        sortBy: String = "relevance", // relevance, price_asc, price_desc, commission, rating, sales
        page: Int = 1,
        limit: Int = 20
    ): ProductListResponse {
        return httpClient.get("api/v1/marketplace/products") {
            categoryId?.let { parameter("category", it) }
            minPrice?.let { parameter("min_price", it) }
            maxPrice?.let { parameter("max_price", it) }
            minCommission?.let { parameter("min_commission", it) }
            search?.let { parameter("search", it) }
            parameter("sort_by", sortBy)
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }
    
    /**
     * Détails d'un produit
     * GET /api/v1/marketplace/products/{id}
     */
    suspend fun getProduct(productId: String): MarketplaceProduct {
        return httpClient.get("api/v1/marketplace/products/$productId").body()
    }
    
    /**
     * Produits mis en avant
     * GET /api/v1/marketplace/products/featured
     */
    suspend fun getFeaturedProducts(limit: Int = 10): List<MarketplaceProduct> {
        return httpClient.get("api/v1/marketplace/products/featured") {
            parameter("limit", limit)
        }.body()
    }
    
    // ============================================
    // CATEGORIES
    // ============================================
    
    /**
     * Liste des catégories
     * GET /api/v1/marketplace/categories
     */
    suspend fun getCategories(parentId: String? = null): List<ProductCategory> {
        return httpClient.get("api/v1/marketplace/categories") {
            parentId?.let { parameter("parent_id", it) }
        }.body()
    }
    
    // ============================================
    // PROMOTIONS
    // ============================================
    
    /**
     * Créer une promotion (lier vidéo → produit)
     * POST /api/v1/promotions
     */
    suspend fun createPromotion(request: CreatePromotionRequest): ProductPromotion {
        return httpClient.post("api/v1/promotions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    /**
     * Obtenir la promotion liée à un post
     * GET /api/v1/promotions/post/{post_id}
     */
    suspend fun getPromotionByPost(postId: String): ProductPromotion {
        return httpClient.get("api/v1/promotions/post/$postId").body()
    }
    
    /**
     * Promotions d'un produit
     * GET /api/v1/promotions/product/{product_id}
     */
    suspend fun getPromotionsByProduct(productId: String): List<ProductPromotion> {
        return httpClient.get("api/v1/promotions/product/$productId").body()
    }
    
    /**
     * Mes promotions
     * GET /api/v1/promotions/user/{user_id}
     */
    suspend fun getMyPromotions(userId: String): List<ProductPromotion> {
        return httpClient.get("api/v1/promotions/user/$userId").body()
    }
    
    // ============================================
    // AFFILIATE SALES
    // ============================================
    
    /**
     * Mes ventes affiliées
     * GET /api/v1/affiliates/sales
     */
    suspend fun getMySales(status: String? = null): List<AffiliateSale> {
        return httpClient.get("api/v1/affiliates/sales") {
            status?.let { parameter("status", it) }
        }.body()
    }
    
    // ============================================
    // WALLET
    // ============================================
    
    /**
     * Mon portefeuille
     * GET /api/v1/wallet
     */
    suspend fun getMyWallet(): PromoterWallet {
        return httpClient.get("api/v1/wallet").body()
    }
    
    /**
     * Historique des transactions
     * GET /api/v1/wallet/transactions
     */
    suspend fun getWalletTransactions(limit: Int = 50): List<WalletTransaction> {
        return httpClient.get("api/v1/wallet/transactions") {
            parameter("limit", limit)
        }.body()
    }
    
    // ============================================
    // WITHDRAWALS
    // ============================================
    
    /**
     * Demander un retrait
     * POST /api/v1/wallet/withdraw
     */
    suspend fun requestWithdrawal(request: CreateWithdrawalRequest): WithdrawalRequest {
        return httpClient.post("api/v1/wallet/withdraw") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    /**
     * Mes demandes de retrait
     * GET /api/v1/wallet/withdrawals
     */
    suspend fun getMyWithdrawals(): List<WithdrawalRequest> {
        return httpClient.get("api/v1/wallet/withdrawals").body()
    }
}

/**
 * Réponse de liste de produits avec pagination
 */
@kotlinx.serialization.Serializable
data class ProductListResponse(
    val items: List<MarketplaceProduct> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    @kotlinx.serialization.SerialName("total_pages") val totalPages: Int = 0
)
