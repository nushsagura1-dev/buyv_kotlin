package com.project.e_commerce.domain.model

import kotlinx.serialization.Serializable

/**
 * Modèle de commande partagé entre Android et iOS.
 * 
 * Représente une commande passée par un utilisateur.
 * Les timestamps sont stockés en millisecondes (Long) pour la compatibilité multiplateforme.
 */
@Serializable
data class Order(
    val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val subtotal: Double = 0.0,
    val shipping: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val shippingAddress: Address? = null,
    val paymentMethod: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val estimatedDelivery: Long = 0L,
    val trackingNumber: String? = null,
    val notes: String = "",
    val promoterUid: String? = null,
    val paymentIntentId: String? = null
)

/**
 * Élément d'une commande.
 */
@Serializable
data class OrderItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val size: String? = null,
    val color: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val promoterUid: String? = null,
    val isPromotedProduct: Boolean = false
)

/**
 * Statut de commande.
 * 
 * Représente les différents états possibles d'une commande.
 */
@Serializable
enum class OrderStatus(val displayName: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    CANCELED("Canceled"),
    RETURNED("Returned"),
    REFUNDED("Refunded");

    companion object {
        /**
         * Convertit une chaîne en OrderStatus.
         * Retourne PENDING si la valeur n'est pas reconnue.
         */
        fun fromString(status: String): OrderStatus {
            return entries.find { it.name.equals(status, ignoreCase = true) } ?: PENDING
        }
    }
}

/**
 * Adresse de livraison.
 * 
 * Tous les champs doivent être validés avant l'envoi au backend.
 */
@Serializable
data class Address(
    val id: String = "",
    val name: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "",
    val phone: String = "",
    val isDefault: Boolean = false
)
