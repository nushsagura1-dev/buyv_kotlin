package com.project.e_commerce.domain.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Modèle de panier utilisateur.
 * 
 * Représente le panier d'achat d'un utilisateur avec tous ses articles.
 */
@Serializable
data class Cart(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    /**
     * Calcule le total du panier.
     */
    fun calculateTotal(): Double {
        return items.sumOf { it.totalPrice }
    }
    
    /**
     * Compte le nombre total d'articles (en tenant compte des quantités).
     */
    fun getTotalItemCount(): Int {
        return items.sumOf { it.quantity }
    }
}

/**
 * Élément du panier.
 */
@Serializable
data class CartItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val size: String? = null,
    val color: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val addedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val promoterUid: String? = null,
    val reelId: String? = null
) {
    /**
     * Calcule le prix total de cet article (prix × quantité).
     */
    val totalPrice: Double
        get() = price * quantity
}

/**
 * Statistiques du panier pour l'UI.
 */
@Serializable
data class CartStats(
    val itemCount: Int = 0,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val shipping: Double = 0.0,
    val total: Double = 0.0
)
