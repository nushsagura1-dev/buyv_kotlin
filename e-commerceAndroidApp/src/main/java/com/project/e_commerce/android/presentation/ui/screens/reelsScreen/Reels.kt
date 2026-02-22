package com.project.e_commerce.android.presentation.ui.screens.reelsScreen

import android.net.Uri
import com.project.e_commerce.android.R
import com.project.e_commerce.android.data.model.CartStats
import com.project.e_commerce.domain.model.Product

data class Reels(
    val id: String = "",
    val userId: String = "", // Add userId to identify reel owner
    val userName: String = "",
    val userImage: Int = 0,
    val video: Uri? = null,
    val images: List<Uri>? = null,
    val fallbackImageRes: Int = R.drawable.img_2,
    val contentDescription: String = "",
    val love: LoveItem = LoveItem(),
    val ratings: List<Ratings> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val newComment: NewComment = NewComment(),
    val isDialogShown: Boolean = false,
    val isLoading: Boolean = true,
    val isError: Boolean = true,
    val numberOfCart: Int = 0, // Keep for backward compatibility
    val numberOfComments: Int = 0,

    // NEW: Cart-related fields
    val cartStats: CartStats = CartStats(),
    val isInCurrentUserCart: Boolean = false,

    val productName: String = "",
    val productPrice: String = "",
    val productImage: String = "",

    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val rating: Double = 0.0,
    
    // NEW: Marketplace product linking (Phase 5)
    val marketplaceProductId: String? = null,
    val marketplaceProductName: String? = null,
    val marketplaceProductPrice: Double? = null,
    val marketplaceCommissionRate: Double? = null,
    
    // Bridge: post UID for likes/comments/bookmarks
    val postUid: String? = null,
    val isBookmarked: Boolean = false,
) {
    companion object {
        fun fromProduct(product: Product): Reels {
            val firstImageUrl = product.productImages.firstOrNull().orEmpty()
            
            // For now, use a placeholder that will be replaced with real user data
            // The real username should come from the user's profile in Firebase
            val placeholderUsername = if (product.userId.isNotEmpty()) {
                "User_${product.userId.take(8)}" // Generate a readable placeholder
            } else {
                "Unknown User"
            }
            
            // Safely parse video URL
            val videoUri = try {
                if (product.reelVideoUrl.isNotEmpty() && product.reelVideoUrl.startsWith("http")) {
                    Uri.parse(product.reelVideoUrl)
                } else {
                    null
                }
            } catch (e: Exception) {
                // Log error and return null for video
                android.util.Log.e("Reels", "Failed to parse video URL: ${product.reelVideoUrl}", e)
                null
            }
            
            return Reels(
                id = product.id,
                userId = product.userId, // Keep empty if no real user — UI will hide follow button
                userName = placeholderUsername, // Will be replaced with real username from user profile
                userImage = R.drawable.profile, // كان String في القديم؛ دلوقتي UI متوقع Resource
                video = videoUri,
                images = product.productImages.takeIf { it.isNotEmpty() }?.mapNotNull { url ->
                    try {
                        if (url.isNotEmpty() && url.startsWith("http")) {
                            Uri.parse(url)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Reels", "Failed to parse image URL: $url", e)
                        null
                    }
                },
                contentDescription = enhanceWithHashtags(
                    product.reelTitle.ifEmpty { product.description },
                    product.name
                ),
                love = LoveItem(number = 0, isLoved = false),
                ratings = emptyList(),
                comments = emptyList(),
                newComment = NewComment(),
                isDialogShown = false,
                isLoading = false,
                isError = false,
                numberOfCart = 0,
                numberOfComments = 0,

                // NEW: Initialize cart stats
                cartStats = CartStats(productId = product.id),

                // Use actual product data
                productName = product.name,
                productPrice = product.price,
                productImage = firstImageUrl,

                sizes = product.sizeColorData.mapNotNull { it["size"] }.distinct(),
                colors = product.sizeColorData.mapNotNull { it["color"] }.distinct(),
                rating = product.rating
            )
        }

        private fun enhanceWithHashtags(description: String, productName: String): String {
            // Clean up product name words and filter out short/common words
            val cleanedWords = productName.lowercase()
                .split(" ")
                .filter { it.length > 2 && it !in listOf("the", "and", "for", "with", "from") }
                .map { "#$it" }

            // Add some common e-commerce hashtags
            val commonHashtags =
                listOf("#fashion", "#lifestyle", "#shopping", "#style", "#trending")

            // Combine original description with hashtags
            val allHashtags = (cleanedWords + commonHashtags.take(2)).joinToString(" ")

            return if (description.isNotBlank()) {
                "$description $allHashtags"
            } else {
                "Check out this amazing product! $allHashtags"
            }
        }
    }
}

data class LoveItem(
    val number: Int = 0,
    val isLoved: Boolean = false,
)

data class NewComment(
    val comment: String = ""
)

data class Comment(
    val id: String = "",
    val userId: String = "", // Add userId to identify comment author
    val userName: String = "",
    val comment: String = "",
    val time: String = "",
    val reply: List<Comment> = emptyList(),
    val isLoved: Boolean = false,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isReplyShown: Boolean = false,
)

data class Ratings(
    val userId: String = "", // Add userId to identify rating author
    val userName: String = "",
    val review: String = "",
    val rate: Int = 0,
    val time: String = ""
)
