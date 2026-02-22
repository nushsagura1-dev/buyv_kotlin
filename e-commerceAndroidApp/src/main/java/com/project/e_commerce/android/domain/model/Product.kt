package com.project.e_commerce.android.domain.model

import com.google.firebase.Timestamp

data class Product(
    val id: String = "",
    val userId: String = "", // Add userId to identify product owner
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
    val sizeColorData: List<Map<String, Any>> = emptyList(),
    val createdAt: Timestamp? = null
)
