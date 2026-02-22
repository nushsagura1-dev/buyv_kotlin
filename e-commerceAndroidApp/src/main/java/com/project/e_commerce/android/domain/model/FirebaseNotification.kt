package com.project.e_commerce.android.domain.model

import com.google.firebase.Timestamp

// Main notification model for Firebase Firestore
data class FirebaseNotification(
    val id: String = "",
    val userId: String = "", // Recipient user ID
    val type: NotificationType = NotificationType.SYSTEM,
    val category: NotificationCategory = NotificationCategory.GENERAL,
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val priority: NotificationPriority = NotificationPriority.MEDIUM,
    val data: Map<String, Any> = emptyMap(), // Custom payload for deep linking
    val actionButtons: List<NotificationAction> = emptyList(),
    val deepLink: String? = null, // Deep link to specific screen
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null, // Optional expiration
    val senderId: String? = null, // For user-generated notifications (likes, follows)
    val senderName: String? = null,
    val senderImageUrl: String? = null,
    val metadata: Map<String, Any> = emptyMap(), // Additional data
    val isSystemNotification: Boolean = true // true for system, false for user-generated
)

// Notification types
enum class NotificationType {
    // E-commerce & Orders
    ORDER_CONFIRMATION,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    REFUND_PROCESSED,

    // Shopping & Cart
    CART_ABANDONMENT,
    PRICE_DROP,
    BACK_IN_STOCK,
    LOW_STOCK_WARNING,
    FLASH_SALE,

    // Social Interactions
    NEW_FOLLOWER,
    NEW_LIKE,
    NEW_COMMENT,
    COMMENT_REPLY,
    REEL_SHARED,
    MENTION,

    // Content & Reels
    REEL_APPROVED,
    CONTENT_MILESTONE,
    TRENDING_REEL,
    UPLOAD_COMPLETE,

    // Business & Seller
    PRODUCT_SOLD,
    EARNINGS_UPDATE,
    PRODUCT_REVIEW,
    STOCK_ALERT,

    // System & Account
    WELCOME_MESSAGE,
    PROFILE_UPDATED,
    SECURITY_ALERT,
    PASSWORD_CHANGED,
    ACCOUNT_VERIFIED,

    // Promotions & Marketing
    EXCLUSIVE_OFFER,
    BIRTHDAY_OFFER,
    LOYALTY_REWARD,
    NEW_COLLECTION,
    WEEKLY_DEALS,

    // App Updates
    APP_UPDATE,
    MAINTENANCE_NOTICE,
    FEATURE_ANNOUNCEMENT,

    // General
    SYSTEM
}

// Notification categories for filtering
enum class NotificationCategory {
    ORDERS_SHIPPING,    // 📦
    SOCIAL_ACTIVITY,    // 👥  
    PROMOTIONS_DEALS,   // 🎁
    ACCOUNT_SECURITY,   // 🔒
    APP_UPDATES,        // 📱
    GENERAL             // 🔔
}

// Notification priority levels
enum class NotificationPriority {
    HIGH,    // Orders, payments, security alerts
    MEDIUM,  // Social interactions, promotions  
    LOW      // App updates, general announcements
}

// Action buttons for notifications
data class NotificationAction(
    val id: String = "",
    val label: String = "",
    val action: String = "", // Deep link or action identifier
    val style: ActionStyle = ActionStyle.DEFAULT
)

enum class ActionStyle {
    DEFAULT,
    PRIMARY,
    DESTRUCTIVE
}

// User notification settings
data class NotificationSettings(
    val userId: String = "",
    val orderNotifications: Boolean = true,
    val socialNotifications: Boolean = true,
    val promotionNotifications: Boolean = true,
    val securityNotifications: Boolean = true,
    val appUpdateNotifications: Boolean = true,
    val emailNotifications: Boolean = false,
    val pushNotifications: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00", // 24-hour format
    val quietHoursEnd: String = "08:00",
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val updatedAt: Timestamp = Timestamp.now()
)

// Notification template for reusable notifications
data class NotificationTemplate(
    val id: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val titleTemplate: String = "",
    val bodyTemplate: String = "",
    val category: NotificationCategory = NotificationCategory.GENERAL,
    val priority: NotificationPriority = NotificationPriority.MEDIUM,
    val defaultActions: List<NotificationAction> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now()
)