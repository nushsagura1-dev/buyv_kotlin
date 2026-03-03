package com.project.e_commerce.android.data.model

data class User(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String? = null,
    val profileImageUrl: String? = null,
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val bio: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val reelsCount: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
