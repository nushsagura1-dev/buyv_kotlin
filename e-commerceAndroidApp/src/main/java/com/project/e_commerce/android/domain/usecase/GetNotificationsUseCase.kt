package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.model.FirebaseNotification
import com.project.e_commerce.android.domain.model.NotificationCategory
import com.project.e_commerce.android.domain.model.NotificationType
import com.project.e_commerce.android.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

class GetNotificationsUseCase(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(userId: String): Flow<List<FirebaseNotification>> {
        return notificationRepository.getNotifications(userId)
    }
}

class GetNotificationsByTypeUseCase(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(userId: String, type: NotificationType): Flow<List<FirebaseNotification>> {
        return notificationRepository.getNotificationsByType(userId, type)
    }
}

class GetNotificationsByCategoryUseCase(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(
        userId: String,
        category: NotificationCategory
    ): Flow<List<FirebaseNotification>> {
        return notificationRepository.getNotificationsByCategory(userId, category)
    }
}

class GetUnreadNotificationsUseCase(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(userId: String): Flow<List<FirebaseNotification>> {
        return notificationRepository.getUnreadNotifications(userId)
    }
}

class GetUnreadCountUseCase(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(userId: String): Flow<Int> {
        return notificationRepository.getUnreadCount(userId)
    }
}

class MarkAsReadUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(userId: String, notificationId: String): Result<Unit> {
        return notificationRepository.markAsRead(userId, notificationId)
    }
}

class MarkAllAsReadUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return notificationRepository.markAllAsRead(userId)
    }
}

class DeleteNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(userId: String, notificationId: String): Result<Unit> {
        return notificationRepository.deleteNotification(userId, notificationId)
    }
}

class CreateNotificationUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(notification: FirebaseNotification): Result<Unit> {
        return notificationRepository.createNotification(notification)
    }
}