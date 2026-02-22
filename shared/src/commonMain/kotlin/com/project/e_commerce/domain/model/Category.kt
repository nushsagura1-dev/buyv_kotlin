package com.project.e_commerce.domain.model

import kotlinx.serialization.Serializable

/**
 * Modèle de catégorie de produit.
 * 
 * Représente une catégorie dans laquelle les produits peuvent être classés.
 */
@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    val image: String = ""
)
