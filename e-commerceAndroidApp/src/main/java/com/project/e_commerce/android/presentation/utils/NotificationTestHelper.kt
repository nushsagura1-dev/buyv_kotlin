package com.project.e_commerce.android.presentation.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.model.FirebaseNotification
import com.project.e_commerce.android.domain.model.NotificationCategory
import com.project.e_commerce.android.domain.model.NotificationPriority
import com.project.e_commerce.android.domain.model.NotificationType
import com.project.e_commerce.android.domain.repository.NotificationRepository

/**
 * Helper class for testing and creating sample notifications during development
 */
class NotificationTestHelper(
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "NotificationTestHelper"
    }

    /**
     * Creates a test order notification
     */
    suspend fun createTestOrderNotification(): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        val notification = FirebaseNotification(
            userId = currentUser.uid,
            type = NotificationType.ORDER_SHIPPED,
            category = NotificationCategory.ORDERS_SHIPPING,
            title = "🚚 Your Order is on the Way!",
            body = "Your order #12345 has been shipped and will arrive in 2-3 business days. Track your package for real-time updates.",
            priority = NotificationPriority.HIGH,
            data = mapOf(
                "orderId" to "12345",
                "trackingNumber" to "TRK123456789",
                "estimatedDelivery" to "2024-01-15"
            ),
            deepLink = "ecommerce://orders/12345",
            createdAt = Timestamp.now(),
            isSystemNotification = true
        )

        Log.d(TAG, "Creating test order notification")
        return notificationRepository.createNotification(notification)
    }

    /**
     * Creates a test social notification
     */
    suspend fun createTestSocialNotification(): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        val notification = FirebaseNotification(
            userId = currentUser.uid,
            type = NotificationType.NEW_LIKE,
            category = NotificationCategory.SOCIAL_ACTIVITY,
            title = "❤️ New Like!",
            body = "Sarah Johnson liked your recent reel about 'Summer Fashion Trends'",
            priority = NotificationPriority.MEDIUM,
            data = mapOf(
                "likedBy" to "sarah_johnson",
                "contentId" to "reel_789",
                "contentType" to "reel"
            ),
            deepLink = "ecommerce://reels/789",
            senderId = "sarah_johnson_uid",
            senderName = "Sarah Johnson",
            senderImageUrl = "https://example.com/profile/sarah.jpg",
            createdAt = Timestamp.now(),
            isSystemNotification = false
        )

        Log.d(TAG, "Creating test social notification")
        return notificationRepository.createNotification(notification)
    }

    /**
     * Creates a test promotion notification
     */
    suspend fun createTestPromotionNotification(): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        val notification = FirebaseNotification(
            userId = currentUser.uid,
            type = NotificationType.FLASH_SALE,
            category = NotificationCategory.PROMOTIONS_DEALS,
            title = "⚡ Flash Sale Alert!",
            body = "50% OFF on Electronics! Limited time offer ending in 6 hours. Shop now and save big!",
            priority = NotificationPriority.HIGH,
            imageUrl = "https://example.com/flash-sale-banner.jpg",
            data = mapOf(
                "discount" to "50",
                "category" to "electronics",
                "endTime" to "2024-01-10T18:00:00Z",
                "minPurchase" to "100"
            ),
            deepLink = "ecommerce://products/category/electronics?sale=flash",
            createdAt = Timestamp.now(),
            expiresAt = Timestamp(Timestamp.now().seconds + 21600, 0), // 6 hours from now
            isSystemNotification = true
        )

        Log.d(TAG, "Creating test promotion notification")
        return notificationRepository.createNotification(notification)
    }

    /**
     * Creates a test security notification
     */
    suspend fun createTestSecurityNotification(): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        val notification = FirebaseNotification(
            userId = currentUser.uid,
            type = NotificationType.SECURITY_ALERT,
            category = NotificationCategory.ACCOUNT_SECURITY,
            title = "🔒 Security Alert",
            body = "New login detected from iPhone in New York, NY. If this wasn't you, please secure your account immediately.",
            priority = NotificationPriority.HIGH,
            data = mapOf(
                "device" to "iPhone 15 Pro",
                "location" to "New York, NY",
                "ipAddress" to "192.168.1.100",
                "timestamp" to System.currentTimeMillis().toString()
            ),
            deepLink = "ecommerce://profile/security",
            createdAt = Timestamp.now(),
            isSystemNotification = true
        )

        Log.d(TAG, "Creating test security notification")
        return notificationRepository.createNotification(notification)
    }

    /**
     * Creates multiple test notifications of different types
     */
    suspend fun createMultipleTestNotifications(): Result<Unit> {
        return try {
            createTestOrderNotification()
            createTestSocialNotification()
            createTestPromotionNotification()
            createTestSecurityNotification()
            
            // Create a few more varied notifications
            createTestBackInStockNotification()
            createTestNewFollowerNotification()
            
            Log.d(TAG, "✅ Created multiple test notifications successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating test notifications", e)
            Result.failure(e)
        }
    }

    private suspend fun createTestBackInStockNotification(): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        val notification = FirebaseNotification(
            userId = currentUser.uid,
            type = NotificationType.BACK_IN_STOCK,
            category = NotificationCategory.ORDERS_SHIPPING,
            title = "📦 Back in Stock!",
            body = "Good news! The 'Wireless Bluetooth Headphones' you wishlisted is now available. Get it before it's gone again!",
            priority = NotificationPriority.MEDIUM,
            data = mapOf(
                "productId" to "prod_456",
                "productName" to "Wireless Bluetooth Headphones",
                "price" to "79.99"
            ),
            deepLink = "ecommerce://products/prod_456",
            createdAt = Timestamp.now(),
            isSystemNotification = true
        )

        return notificationRepository.createNotification(notification)
    }

    private suspend fun createTestNewFollowerNotification(): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        val notification = FirebaseNotification(
            userId = currentUser.uid,
            type = NotificationType.NEW_FOLLOWER,
            category = NotificationCategory.SOCIAL_ACTIVITY,
            title = "👥 New Follower!",
            body = "Mike Chen started following you. Check out their profile and latest posts!",
            priority = NotificationPriority.MEDIUM,
            data = mapOf(
                "followerId" to "mike_chen_uid",
                "followerUsername" to "mike_chen"
            ),
            deepLink = "ecommerce://profile/mike_chen_uid",
            senderId = "mike_chen_uid",
            senderName = "Mike Chen",
            senderImageUrl = "https://example.com/profile/mike.jpg",
            createdAt = Timestamp.now(),
            isSystemNotification = false
        )

        return notificationRepository.createNotification(notification)
    }
}