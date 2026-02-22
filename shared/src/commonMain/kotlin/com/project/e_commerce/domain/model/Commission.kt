package com.project.e_commerce.domain.model

import kotlinx.serialization.Serializable

/**
 * Modèle de commission partagé entre Android et iOS.
 * 
 * Représente une commission gagnée par un vendeur ou promoteur.
 */
@Serializable
data class Commission(
    val id: Int = 0,
    val userId: String = "",
    val orderId: String = "",
    val orderItemId: String? = null,
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val commissionRate: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val status: String = "pending", // pending, approved, rejected, paid
    val createdAt: String = "",
    val updatedAt: String = "",
    val paidAt: String? = null,
    val metadata: Map<String, String>? = null
)

/**
 * Réponse pour les opérations de commission.
 */
@Serializable
data class CommissionResponse(
    val commissions: List<Commission> = emptyList(),
    val total: Int = 0
)

/**
 * Statistiques de commission.
 */
@Serializable
data class CommissionStats(
    val totalCommissions: Int = 0,
    val totalAmount: Double = 0.0,
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0,
    val paidCount: Int = 0
)
