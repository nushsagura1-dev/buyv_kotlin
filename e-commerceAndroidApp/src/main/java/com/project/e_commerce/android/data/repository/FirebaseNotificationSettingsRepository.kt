package com.project.e_commerce.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.e_commerce.android.domain.model.NotificationCategory
import com.project.e_commerce.android.domain.model.NotificationSettings
import com.project.e_commerce.android.domain.model.NotificationType
import com.project.e_commerce.android.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.tasks.await

class FirebaseNotificationSettingsRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : NotificationSettingsRepository {

    companion object {
        private const val TAG = "FirebaseNotificationSettingsRepo"
        private const val NOTIFICATION_SETTINGS_COLLECTION = "notification_settings"
    }

    override suspend fun getNotificationSettings(userId: String): Result<NotificationSettings> =
        try {
            val document = firestore.collection(NOTIFICATION_SETTINGS_COLLECTION)
                .document(userId)
                .get()
                .await()

            val settings = if (document.exists()) {
                document.toObject(NotificationSettings::class.java)
                    ?: getDefaultNotificationSettings(userId)
            } else {
                // Create default settings if not exists
                val defaultSettings = getDefaultNotificationSettings(userId)
                updateNotificationSettings(defaultSettings)
                defaultSettings
            }

            Log.d(TAG, "Retrieved notification settings for user: $userId")
            Result.success(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification settings", e)
            Result.failure(e)
        }

    override suspend fun updateNotificationSettings(settings: NotificationSettings): Result<Unit> =
        try {
            firestore.collection(NOTIFICATION_SETTINGS_COLLECTION)
                .document(settings.userId)
                .set(settings)
                .await()

            Log.d(TAG, "Updated notification settings for user: ${settings.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification settings", e)
            Result.failure(e)
        }

    override suspend fun isNotificationTypeEnabled(
        userId: String,
        type: NotificationType
    ): Result<Boolean> = try {
        val settingsResult = getNotificationSettings(userId)

        if (settingsResult.isSuccess) {
            val settings = settingsResult.getOrNull()!!
            val isEnabled = when (type) {
                // E-commerce & Orders
                NotificationType.ORDER_CONFIRMATION,
                NotificationType.ORDER_SHIPPED,
                NotificationType.ORDER_DELIVERED,
                NotificationType.ORDER_CANCELLED,
                NotificationType.PAYMENT_SUCCESS,
                NotificationType.PAYMENT_FAILED,
                NotificationType.REFUND_PROCESSED -> settings.orderNotifications

                // Shopping & Cart  
                NotificationType.CART_ABANDONMENT,
                NotificationType.PRICE_DROP,
                NotificationType.BACK_IN_STOCK,
                NotificationType.LOW_STOCK_WARNING,
                NotificationType.FLASH_SALE -> settings.orderNotifications

                // Social Interactions
                NotificationType.NEW_FOLLOWER,
                NotificationType.NEW_LIKE,
                NotificationType.NEW_COMMENT,
                NotificationType.COMMENT_REPLY,
                NotificationType.REEL_SHARED,
                NotificationType.MENTION -> settings.socialNotifications

                // Content & Reels
                NotificationType.REEL_APPROVED,
                NotificationType.CONTENT_MILESTONE,
                NotificationType.TRENDING_REEL,
                NotificationType.UPLOAD_COMPLETE -> settings.socialNotifications

                // Business & Seller
                NotificationType.PRODUCT_SOLD,
                NotificationType.EARNINGS_UPDATE,
                NotificationType.PRODUCT_REVIEW,
                NotificationType.STOCK_ALERT -> settings.orderNotifications

                // System & Account
                NotificationType.WELCOME_MESSAGE,
                NotificationType.PROFILE_UPDATED,
                NotificationType.SECURITY_ALERT,
                NotificationType.PASSWORD_CHANGED,
                NotificationType.ACCOUNT_VERIFIED -> settings.securityNotifications

                // Promotions & Marketing
                NotificationType.EXCLUSIVE_OFFER,
                NotificationType.BIRTHDAY_OFFER,
                NotificationType.LOYALTY_REWARD,
                NotificationType.NEW_COLLECTION,
                NotificationType.WEEKLY_DEALS -> settings.promotionNotifications

                // App Updates
                NotificationType.APP_UPDATE,
                NotificationType.MAINTENANCE_NOTICE,
                NotificationType.FEATURE_ANNOUNCEMENT -> settings.appUpdateNotifications

                // General
                NotificationType.SYSTEM -> true // System notifications are always enabled
            }

            Log.d(TAG, "Notification type $type enabled: $isEnabled for user: $userId")
            Result.success(isEnabled)
        } else {
            Result.failure(settingsResult.exceptionOrNull() ?: Exception("Failed to get settings"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error checking notification type enabled", e)
        Result.failure(e)
    }

    override suspend fun isNotificationCategoryEnabled(
        userId: String,
        category: NotificationCategory
    ): Result<Boolean> = try {
        val settingsResult = getNotificationSettings(userId)

        if (settingsResult.isSuccess) {
            val settings = settingsResult.getOrNull()!!
            val isEnabled = when (category) {
                NotificationCategory.ORDERS_SHIPPING -> settings.orderNotifications
                NotificationCategory.SOCIAL_ACTIVITY -> settings.socialNotifications
                NotificationCategory.PROMOTIONS_DEALS -> settings.promotionNotifications
                NotificationCategory.ACCOUNT_SECURITY -> settings.securityNotifications
                NotificationCategory.APP_UPDATES -> settings.appUpdateNotifications
                NotificationCategory.GENERAL -> true // General notifications are always enabled
            }

            Log.d(TAG, "Notification category $category enabled: $isEnabled for user: $userId")
            Result.success(isEnabled)
        } else {
            Result.failure(settingsResult.exceptionOrNull() ?: Exception("Failed to get settings"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error checking notification category enabled", e)
        Result.failure(e)
    }

    private fun getDefaultNotificationSettings(userId: String): NotificationSettings {
        return NotificationSettings(
            userId = userId,
            orderNotifications = true,
            socialNotifications = true,
            promotionNotifications = true,
            securityNotifications = true,
            appUpdateNotifications = true,
            emailNotifications = false,
            pushNotifications = true,
            quietHoursEnabled = false,
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            notificationSound = true,
            notificationVibration = true,
            updatedAt = com.google.firebase.Timestamp.now()
        )
    }
}