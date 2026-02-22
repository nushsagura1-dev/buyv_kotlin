package com.project.e_commerce.data.mappers

import com.project.e_commerce.domain.model.Product
import com.project.e_commerce.domain.model.marketplace.MarketplaceProduct

/**
 * Convertit un produit du marketplace (DTO backend) vers le modèle de domaine Product (UI).
 */
fun MarketplaceProduct.toProduct(): Product {
    return Product(
        id = id,
        userId = promoterUserId ?: "", // UID du promoteur pour le split de commission
        name = name,
        description = description ?: "",
        price = sellingPrice.toString(),
        categoryId = categoryId ?: "",
        categoryName = categoryName ?: "",
        quantity = "100", // Par défaut pour CJ
        rating = averageRating,
        productImages = images,
        image = mainImageUrl ?: images.firstOrNull() ?: "",
        reelTitle = name, // Fallback
        reelVideoUrl = reelVideoUrl ?: "", // URL vidéo Cloudinary du promoteur
        searchQuery = name.lowercase(),
        tags = tags.joinToString(","),
        sizeColorData = emptyList(), // CJ gère les variantes différemment, à implémenter si besoin
        createdAt = try {
            0L 
        } catch (e: Exception) { 0L },
        originalPrice = originalPrice,
        commissionRate = commissionRate,
        postUid = postUid ?: "",  // UID du post lié (Firebase post_id de la première promotion)
        likesCount = postLikesCount ?: 0,  // Nombre de likes du post lié
        isLiked = isLiked,        // État like de l'utilisateur courant (depuis le backend)
        isBookmarked = isBookmarked  // État bookmark de l'utilisateur courant (depuis le backend)
    )
}

/**
 * Extension pour convertir une liste
 */
fun List<MarketplaceProduct>.toProductList(): List<Product> {
    return map { it.toProduct() }
}
