package com.project.e_commerce.android.domain.repository

import com.project.e_commerce.android.domain.model.FirebaseNotification
import com.project.e_commerce.android.domain.model.NotificationCategory
import com.project.e_commerce.android.domain.model.NotificationSettings
import com.project.e_commerce.android.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    // Get notifications
    fun getNotifications(userId: String): Flow<List<FirebaseNotification>>

    fun getNotificationsByCategory(
        userId: String,
        category: NotificationCategory
    ): Flow<List<FirebaseNotification>>

    fun getNotificationsByType(
        userId: String,
        type: NotificationType
    ): Flow<List<FirebaseNotification>>

    fun getUnreadNotifications(userId: String): Flow<List<FirebaseNotification>>

    suspend fun getNotificationById(
        userId: String,
        notificationId: String
    ): Result<FirebaseNotification?>

    // Notification actions
    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit>

    suspend fun markAllAsRead(userId: String): Result<Unit>

    suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit>

    suspend fun deleteAllNotifications(userId: String): Result<Unit>

    // Create notifications (for system/admin use)
    suspend fun createNotification(notification: FirebaseNotification): Result<Unit>

    suspend fun createBulkNotifications(notifications: List<FirebaseNotification>): Result<Unit>

    // Notification count
    fun getUnreadCount(userId: String): Flow<Int>

    // FCM Token management
    suspend fun updateFCMToken(userId: String, token: String): Result<Unit>

    suspend fun getFCMToken(userId: String): Result<String?>
}

interface NotificationSettingsRepository {

    suspend fun getNotificationSettings(userId: String): Result<NotificationSettings>

    suspend fun updateNotificationSettings(settings: NotificationSettings): Result<Unit>

    suspend fun isNotificationTypeEnabled(userId: String, type: NotificationType): Result<Boolean>

    suspend fun isNotificationCategoryEnabled(
        userId: String,
        category: NotificationCategory
    ): Result<Boolean>
}