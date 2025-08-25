package com.project.e_commerce.android.presentation.ui.screens.reelsScreen

import android.net.Uri
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.viewModel.Product


data class Reels(
    val id: String = "",
    val userId: String = "", // Add userId to identify reel owner
    val userName: String = "",
    val userImage: Int = 0,
    val video: Uri? = null,
    val images: List<Uri>? = null,
    val fallbackImageRes: Int = R.drawable.reelsphoto,
    val contentDescription: String = "",
    val love: LoveItem = LoveItem(),
    val ratings: List<Ratings> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val newComment: NewComment = NewComment(),
    val isDialogShown: Boolean = false,
    val isLoading: Boolean = true,
    val isError: Boolean = true,
    val numberOfCart: Int = 0,
    val numberOfComments: Int = 0,

    val productName: String = "",
    val productPrice: String = "",
    val productImage: String = "",

    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val rating: Double = 0.0,
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
            
            return Reels(
                id = product.id,
                userId = product.userId.ifEmpty { "Jn8DBgNiS7Y4vnUTlmra2vPdf433" }, // Use real user ID instead of non-existent "store_official"
                userName = placeholderUsername, // Will be replaced with real username from user profile
                userImage = R.drawable.profile, // كان String في القديم؛ دلوقتي UI متوقع Resource
                video = product.reelVideoUrl.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) },
                images = product.productImages.takeIf { it.isNotEmpty() }?.map { Uri.parse(it) },
                contentDescription = product.reelTitle.ifEmpty { product.description },
                love = LoveItem(number = 0, isLoved = false),
                ratings = emptyList(),
                comments = emptyList(),
                newComment = NewComment(),
                isDialogShown = false,
                isLoading = false,
                isError = false,
                numberOfCart = 0,
                numberOfComments = 0,

                // لو عندك في Product اسم/سعر، اقدر أربطهم مباشرة.
                // سايبهم فاضيين افتراضيًا عشان مانكسرش لو الـ Product مش فيه الحقول دي.
                productName = "",     // مثال: product.name
                productPrice = "",    // مثال: product.priceFormatted أو product.price.toString()
                productImage = firstImageUrl,

                sizes = emptyList(),  // مثال: product.sizes
                colors = emptyList(), // مثال: product.colors
                rating = 0.0          // مثال: product.rating
            )
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
    val userName: String = "",
    val review: String = "",
    val rate: Int = 0,
    val time: String = ""
)
