package com.project.e_commerce.android.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val email: String,
    val username: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("is_active") val isActive: Boolean = true,
    val bio: String? = null,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("posts_count") val postsCount: Int = 0,
    @SerializedName("reels_count") val reelsCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)
