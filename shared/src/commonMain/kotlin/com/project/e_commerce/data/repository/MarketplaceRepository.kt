package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.MarketplaceApiService
import com.project.e_commerce.data.remote.api.ProductListResponse
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.marketplace.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository pour le Marketplace
 * Gère la logique métier et la couche réseau
 */
class MarketplaceRepository(
    private val apiService: MarketplaceApiService
) {
    
    // ============================================
    // PRODUCTS
    // ============================================
    
    /**
     * Récupère la liste des produits
     */
    fun getProducts(
        categoryId: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minCommission: Double? = null,
        search: String? = null,
        sortBy: String = "relevance",
        page: Int = 1,
        limit: Int = 20
    ): Flow<Result<ProductListResponse>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getProducts(
                categoryId, minPrice, maxPrice, minCommission,
                search, sortBy, page, limit
            )
            emit(Result.Success(response))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    /**
     * Récupère un produit par ID
     */
    fun getProduct(productId: String): Flow<Result<MarketplaceProduct>> = flow {
        emit(Result.Loading)
        try {
            val product = apiService.getProduct(productId)
            emit(Result.Success(product))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    /**
     * Récupère les produits mis en avant
     */
    fun getFeaturedProducts(limit: Int = 10): Flow<Result<List<MarketplaceProduct>>> = flow {
        emit(Result.Loading)
        try {
            val products = apiService.getFeaturedProducts(limit)
            emit(Result.Success(products))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    // ============================================
    // CATEGORIES
    // ============================================
    
    /**
     * Récupère les catégories
     */
    fun getCategories(parentId: String? = null): Flow<Result<List<ProductCategory>>> = flow {
        emit(Result.Loading)
        try {
            val categories = apiService.getCategories(parentId)
            emit(Result.Success(categories))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    // ============================================
    // PROMOTIONS
    // ============================================
    
    /**
     * Crée une promotion (lie un post à un produit)
     */
    fun createPromotion(
        postId: String,
        productId: String
    ): Flow<Result<ProductPromotion>> = flow {
        emit(Result.Loading)
        try {
            val promotion = apiService.createPromotion(
                CreatePromotionRequest(postId, productId)
            )
            emit(Result.Success(promotion))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    /**
     * Récupère la promotion d'un post
     */
    fun getPromotionByPost(postId: String): Flow<Result<ProductPromotion>> = flow {
        emit(Result.Loading)
        try {
            val promotion = apiService.getPromotionByPost(postId)
            emit(Result.Success(promotion))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    /**
     * Récupère mes promotions
     */
    fun getMyPromotions(userId: String): Flow<Result<List<ProductPromotion>>> = flow {
        emit(Result.Loading)
        try {
            val promotions = apiService.getMyPromotions(userId)
            emit(Result.Success(promotions))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    // ============================================
    // SALES
    // ============================================
    
    /**
     * Récupère mes ventes
     */
    fun getMySales(status: String? = null): Flow<Result<List<AffiliateSale>>> = flow {
        emit(Result.Loading)
        try {
            val sales = apiService.getMySales(status)
            emit(Result.Success(sales))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    // ============================================
    // WALLET
    // ============================================
    
    /**
     * Récupère mon portefeuille
     */
    fun getMyWallet(): Flow<Result<PromoterWallet>> = flow {
        emit(Result.Loading)
        try {
            val wallet = apiService.getMyWallet()
            emit(Result.Success(wallet))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    /**
     * Récupère les transactions du portefeuille
     */
    fun getWalletTransactions(limit: Int = 50): Flow<Result<List<WalletTransaction>>> = flow {
        emit(Result.Loading)
        try {
            val transactions = apiService.getWalletTransactions(limit)
            emit(Result.Success(transactions))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    // ============================================
    // WITHDRAWALS
    // ============================================
    
    /**
     * Demande un retrait
     */
    fun requestWithdrawal(
        amount: Double,
        bankName: String,
        bankAccountNumber: String,
        bankAccountHolder: String,
        bankSwiftCode: String? = null
    ): Flow<Result<WithdrawalRequest>> = flow {
        emit(Result.Loading)
        try {
            val withdrawal = apiService.requestWithdrawal(
                CreateWithdrawalRequest(
                    amount, "bank_transfer",
                    bankName, bankAccountNumber,
                    bankAccountHolder, bankSwiftCode
                )
            )
            emit(Result.Success(withdrawal))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
    
    /**
     * Récupère mes demandes de retrait
     */
    fun getMyWithdrawals(): Flow<Result<List<WithdrawalRequest>>> = flow {
        emit(Result.Loading)
        try {
            val withdrawals = apiService.getMyWithdrawals()
            emit(Result.Success(withdrawals))
        } catch (e: Exception) {
            emit(Result.Error(com.project.e_commerce.domain.model.ApiError.fromException(e)))
        }
    }
}
