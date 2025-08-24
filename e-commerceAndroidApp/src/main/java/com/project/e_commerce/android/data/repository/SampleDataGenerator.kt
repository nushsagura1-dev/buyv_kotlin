package com.project.e_commerce.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.e_commerce.android.domain.model.*
import kotlinx.coroutines.tasks.await

class SampleDataGenerator(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    suspend fun generateSampleData() {
        val currentUser = auth.currentUser ?: return
        
        // Generate sample user profile
        val sampleProfile = UserProfile(
            uid = currentUser.uid,
            email = currentUser.email ?: "",
            displayName = "John Doe",
            username = "johndoe",
            bio = "Passionate creator and entrepreneur",
            followersCount = 1250,
            followingCount = 89,
            likesCount = 15420
        )
        
        // Save profile
        firestore.collection("users")
            .document(currentUser.uid)
            .set(sampleProfile)
            .await()
        
        // Generate sample reels
        val sampleReels = listOf(
            UserPost(
                id = "reel1",
                userId = currentUser.uid,
                type = "REEL",  // Changed from PostType.REEL to "REEL" string
                title = "Amazing Product Showcase",
                description = "Check out this incredible product!",
                mediaUrl = "https://example.com/reel1.mp4",
                thumbnailUrl = "https://picsum.photos/300/400?random=1",
                likesCount = 245,
                commentsCount = 23,
                viewsCount = 1250,
                isPublished = true
            ),
            UserPost(
                id = "reel2",
                userId = currentUser.uid,
                type = "REEL",  // Changed from PostType.REEL to "REEL" string
                title = "Product Review",
                description = "Honest review of our latest product",
                mediaUrl = "https://example.com/reel2.mp4",
                thumbnailUrl = "https://picsum.photos/300/400?random=2",
                likesCount = 189,
                commentsCount = 15,
                viewsCount = 890,
                isPublished = true
            ),
            UserPost(
                id = "reel3",
                userId = currentUser.uid,
                type = "REEL",  // Changed from PostType.REEL to "REEL" string
                title = "Behind the Scenes",
                description = "See how we create amazing content",
                mediaUrl = "https://example.com/reel3.mp4",
                thumbnailUrl = "https://picsum.photos/300/400?random=3",
                likesCount = 312,
                commentsCount = 28,
                viewsCount = 1560,
                isPublished = true
            )
        )
        
        // Save reels
        sampleReels.forEach { reel ->
            firestore.collection("posts")
                .document(reel.id)
                .set(reel)
                .await()
        }
        
        // Generate sample products
        val sampleProducts = listOf(
            UserProduct(
                id = "product1",
                userId = currentUser.uid,
                name = "Premium Wireless Headphones",
                description = "High-quality wireless headphones with noise cancellation",
                price = 199.99,
                originalPrice = 249.99,
                images = listOf(
                    "https://picsum.photos/300/300?random=4",
                    "https://picsum.photos/300/300?random=5"
                ),
                category = "Electronics",
                stockQuantity = 15,
                likesCount = 89,
                isPublished = true
            ),
            UserProduct(
                id = "product2",
                userId = currentUser.uid,
                name = "Designer Watch",
                description = "Elegant designer watch for any occasion",
                price = 299.99,
                originalPrice = null,
                images = listOf(
                    "https://picsum.photos/300/300?random=6"
                ),
                category = "Fashion",
                stockQuantity = 8,
                likesCount = 156,
                isPublished = true
            ),
            UserProduct(
                id = "product3",
                userId = currentUser.uid,
                name = "Smart Fitness Tracker",
                description = "Track your fitness goals with this smart device",
                price = 79.99,
                originalPrice = 99.99,
                images = listOf(
                    "https://picsum.photos/300/300?random=7",
                    "https://picsum.photos/300/300?random=8"
                ),
                category = "Health & Fitness",
                stockQuantity = 25,
                likesCount = 203,
                isPublished = true
            )
        )
        
        // Save products
        sampleProducts.forEach { product ->
            firestore.collection("products")
                .document(product.id)
                .set(product)
                .await()
        }
        
        // Generate sample interactions (likes and bookmarks)
        val sampleInteractions = listOf(
            UserInteraction(
                id = "like1",
                userId = currentUser.uid,
                targetId = "reel1",
                targetType = InteractionType.LIKE
            ),
            UserInteraction(
                id = "like2",
                userId = currentUser.uid,
                targetId = "product1",
                targetType = InteractionType.LIKE
            ),
            UserInteraction(
                id = "bookmark1",
                userId = currentUser.uid,
                targetId = "reel2",
                targetType = InteractionType.BOOKMARK
            ),
            UserInteraction(
                id = "bookmark2",
                userId = currentUser.uid,
                targetId = "product2",
                targetType = InteractionType.BOOKMARK
            )
        )
        
        // Save interactions
        sampleInteractions.forEach { interaction ->
            firestore.collection("interactions")
                .document(interaction.id)
                .set(interaction)
                .await()
        }
    }
}
