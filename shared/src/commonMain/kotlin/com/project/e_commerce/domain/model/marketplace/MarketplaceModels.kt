package com.project.e_commerce.domain.model.marketplace

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Produit du Marketplace (CJ Dropshipping)
 */
@Serializable
data class MarketplaceProduct(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    
    // Prix
    @SerialName("original_price") val originalPrice: Double = 0.0,
    @SerialName("selling_price") val sellingPrice: Double = 0.0,
    val currency: String = "USD",
    
    // Commission
    @SerialName("commission_rate") val commissionRate: Double = 0.0,
    @SerialName("commission_amount") val commissionAmount: Double? = null,
    @SerialName("commission_type") val commissionType: String = "percentage",
    
    // Catégorie
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    
    // Images
    @SerialName("main_image_url") val mainImageUrl: String? = null,
    val images: List<String> = emptyList(),
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    
    // CJ Dropshipping
    @SerialName("cj_product_id") val cjProductId: String? = null,
    @SerialName("cj_variant_id") val cjVariantId: String? = null,
    
    // Statistiques
    @SerialName("total_sales") val totalSales: Int = 0,
    @SerialName("total_views") val totalViews: Int = 0,
    @SerialName("total_promotions") val totalPromotions: Int = 0,
    @SerialName("average_rating") val averageRating: Double = 0.0,
    @SerialName("rating_count") val ratingCount: Int = 0,
    
    // Statut
    val status: String = "active",
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("is_choice") val isChoice: Boolean = false,
    
    // Tags
    val tags: List<String> = emptyList(),
    
    // Reel vidéo liée (URL Cloudinary uploadée par le promoteur)
    @SerialName("reel_video_url") val reelVideoUrl: String? = null,
    
    // Promoteur lié (pour le split de commission)
    @SerialName("promoter_user_id") val promoterUserId: String? = null,
    
    // Post lié (pour les commentaires via API)
    @SerialName("post_uid") val postUid: String? = null,
    
    // Likes du post lié (pour affichage du compteur de likes)
    @SerialName("post_likes_count") val postLikesCount: Int? = null,
    
    // Statut de l'utilisateur courant (peuplé côté backend si authentifié)
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("is_bookmarked") val isBookmarked: Boolean = false,
    
    // Timestamps
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    /**
     * Calcule la commission estimée pour ce produit
     */
    fun getEstimatedCommission(): Double {
        return if (commissionType == "percentage") {
            sellingPrice * (commissionRate / 100.0)
        } else {
            commissionAmount ?: 0.0
        }
    }
    
    /**
     * Retourne la commission formatée avec devise
     */
    fun getFormattedCommission(): String {
        return "$${String.format("%.2f", getEstimatedCommission())}"
    }
    
    /**
     * Retourne le prix formaté avec devise
     */
    fun getFormattedPrice(): String {
        return "$${String.format("%.2f", sellingPrice)}"
    }
    
    /**
     * Retourne la réduction en pourcentage
     */
    fun getDiscountPercentage(): Int? {
        return if (originalPrice > sellingPrice) {
            ((originalPrice - sellingPrice) / originalPrice * 100).toInt()
        } else null
    }
    
    /**
     * Vérifie si le produit est disponible
     */
    fun isAvailable(): Boolean {
        return status == "active"
    }
}

/**
 * Catégorie de produits
 */
@Serializable
data class ProductCategory(
    val id: String,
    val name: String,
    @SerialName("name_ar") val nameAr: String? = null,
    val slug: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = ""
)

/**
 * Promotion (lien entre post/reel et produit)
 */
@Serializable
data class ProductPromotion(
    val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("promoter_user_id") val promoterUserId: String,
    @SerialName("is_official") val isOfficial: Boolean = false,
    
    // Statistiques
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("clicks_count") val clicksCount: Int = 0,
    @SerialName("sales_count") val salesCount: Int = 0,
    @SerialName("total_revenue") val totalRevenue: Double = 0.0,
    @SerialName("total_commission_earned") val totalCommissionEarned: Double = 0.0,
    
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

/**
 * Vente affiliée
 */
@Serializable
data class AffiliateSale(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("promotion_id") val promotionId: String? = null,
    @SerialName("promoter_user_id") val promoterUserId: String,
    @SerialName("buyer_user_id") val buyerUserId: String,
    
    // Montants
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("sale_amount") val saleAmount: Double,
    @SerialName("commission_amount") val commissionAmount: Double,
    val currency: String = "USD",
    
    // Statut
    @SerialName("commission_status") val commissionStatus: String,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_reference") val paymentReference: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    
    @SerialName("created_at") val createdAt: String = ""
) {
    fun getFormattedAmount(): String {
        return "$${String.format("%.2f", saleAmount)}"
    }
    
    fun getFormattedCommission(): String {
        return "$${String.format("%.2f", commissionAmount)}"
    }
}

/**
 * Portefeuille promoteur
 */
@Serializable
data class PromoterWallet(
    val id: String,
    @SerialName("user_id") val userId: String,
    
    // Montants
    @SerialName("pending_amount") val pendingAmount: Double = 0.0,
    @SerialName("available_amount") val availableAmount: Double = 0.0,
    @SerialName("withdrawn_amount") val withdrawnAmount: Double = 0.0,
    @SerialName("total_earned") val totalEarned: Double = 0.0,
    val currency: String = "USD",
    
    // Info bancaire
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("bank_account_number") val bankAccountNumber: String? = null,
    @SerialName("bank_account_holder") val bankAccountHolder: String? = null,
    @SerialName("bank_swift_code") val bankSwiftCode: String? = null,
    
    // Statistiques
    @SerialName("total_sales_count") val totalSalesCount: Int = 0,
    @SerialName("last_withdrawal_at") val lastWithdrawalAt: String? = null,
    
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    fun getFormattedPending(): String {
        return "$${String.format("%.2f", pendingAmount)}"
    }
    
    fun getFormattedAvailable(): String {
        return "$${String.format("%.2f", availableAmount)}"
    }
    
    fun getFormattedTotal(): String {
        return "$${String.format("%.2f", totalEarned)}"
    }
    
    fun canWithdraw(): Boolean {
        return availableAmount > 0
    }
}

/**
 * Transaction de portefeuille
 */
@Serializable
data class WalletTransaction(
    val id: String,
    @SerialName("wallet_id") val walletId: String,
    val type: String,
    val amount: Double,
    @SerialName("balance_after") val balanceAfter: Double,
    val currency: String = "USD",
    val description: String? = null,
    @SerialName("reference_type") val referenceType: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("created_at") val createdAt: String = ""
) {
    fun getFormattedAmount(): String {
        val sign = if (amount >= 0) "+" else ""
        return "$sign$${String.format("%.2f", amount)}"
    }
}

/**
 * Demande de retrait
 */
@Serializable
data class WithdrawalRequest(
    val id: String,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("user_id") val userId: String,
    val amount: Double,
    val currency: String = "USD",
    @SerialName("payment_method") val paymentMethod: String = "bank_transfer",
    val status: String,
    @SerialName("processed_by") val processedBy: String? = null,
    @SerialName("processed_at") val processedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    fun getFormattedAmount(): String {
        return "$${String.format("%.2f", amount)}"
    }
    
    fun isPending(): Boolean = status == "pending"
    fun isCompleted(): Boolean = status == "completed"
    fun isRejected(): Boolean = status == "rejected"
}

/**
 * Requête de création de promotion
 */
@Serializable
data class CreatePromotionRequest(
    @SerialName("post_id") val postId: String,
    @SerialName("product_id") val productId: String
)

/**
 * Requête de demande de retrait
 */
@Serializable
data class CreateWithdrawalRequest(
    val amount: Double,
    @SerialName("payment_method") val paymentMethod: String = "bank_transfer",
    @SerialName("bank_name") val bankName: String,
    @SerialName("bank_account_number") val bankAccountNumber: String,
    @SerialName("bank_account_holder") val bankAccountHolder: String,
    @SerialName("bank_swift_code") val bankSwiftCode: String? = null
)

/**
 * Options de tri pour les produits du marketplace
 */
enum class ProductSortBy(val value: String) {
    RELEVANCE("relevance"),
    PRICE_LOW_TO_HIGH("price_asc"),
    PRICE_HIGH_TO_LOW("price_desc"),
    COMMISSION("commission"),
    NEWEST("recent"),
    POPULAR("popular")
}
