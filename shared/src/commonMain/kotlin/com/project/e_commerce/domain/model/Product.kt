package com.project.e_commerce.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Modèle de produit partagé entre Android et iOS.
 * 
 * Représente un produit disponible dans l'application e-commerce.
 * Tous les champs sont serialisables pour permettre la transmission via l'API.
 */
@Serializable
data class Product(
    val id: String = "",
    val userId: String = "", // Identifiant du propriétaire du produit
    val name: String = "",
    val description: String = "",
    val price: String = "0",
    val categoryId: String = "",
    val categoryName: String = "",
    val quantity: String = "0",
    val rating: Double = 0.0,
    val productImages: List<String> = emptyList(),
    val image: String = "",
    val reelTitle: String = "",
    val reelVideoUrl: String = "",
    val searchQuery: String = "",
    val tags: String = "",
    val sizeColorData: List<Map<String, String>> = emptyList(),
    val createdAt: Long = 0L, // Timestamp en millisecondes
    val originalPrice: Double = 0.0,
    val commissionRate: Double = 0.0,
    // User info from PostDto for display in reels
    val username: String = "",
    val displayName: String = "",
    val userProfileImage: String = "",
    // Post type to distinguish reels from product shadow posts
    val postType: String = "",
    // Whether the post is liked/bookmarked by current user
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val postUid: String = ""  // UID du post lié (pour les commentaires via API)
)
