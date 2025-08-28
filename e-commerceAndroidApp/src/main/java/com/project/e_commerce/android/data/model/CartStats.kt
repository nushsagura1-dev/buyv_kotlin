package com.project.e_commerce.android.data.model

data class CartStats(
    val productId: String = "",
    val totalAdded: Int = 0,
    val uniqueUsers: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
