package com.project.e_commerce.android.presentation.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.project.e_commerce.android.domain.model.FirebaseNotification
import com.project.e_commerce.android.domain.model.NotificationCategory
import com.project.e_commerce.android.domain.model.NotificationPriority
import com.project.e_commerce.android.domain.model.NotificationType
import com.project.e_commerce.android.domain.repository.NotificationRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val notificationRepository: NotificationRepository by inject()
    private val notificationManagerService: NotificationManagerService by inject()
    private val auth: FirebaseAuth by inject()

    // Create a coroutine scope for this service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MyFCMService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")
        Log.d(TAG, "FCM Message data: ${remoteMessage.data}")
        Log.d(TAG, "FCM Message notification: ${remoteMessage.notification}")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated, ignoring FCM message")
            return
        }

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data, currentUser.uid)
        }

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            handleNotificationMessage(notification, remoteMessage.data, currentUser.uid)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Update token in repository
            serviceScope.launch {
                try {
                    val result = notificationRepository.updateFCMToken(currentUser.uid, token)
                    if (result.isSuccess) {
                        Log.d(TAG, "FCM token updated successfully in Firestore")
                    } else {
                        Log.e(TAG, "Failed to update FCM token: ${result.exceptionOrNull()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating FCM token", e)
                }
            }
        } else {
            Log.w(TAG, "User not authenticated, cannot update FCM token")
        }
    }

    private fun handleDataMessage(data: Map<String, String>, userId: String) {
        try {
            // Extract notification data from FCM payload
            val notificationId = data["notificationId"] ?: generateNotificationId()
            val title = data["title"] ?: "New Notification"
            val body = data["body"] ?: ""
            val type = data["type"]?.let {
                try {
                    NotificationType.valueOf(it)
                } catch (e: Exception) {
                    NotificationType.SYSTEM
                }
            } ?: NotificationType.SYSTEM
            val category = data["category"]?.let {
                try {
                    NotificationCategory.valueOf(it)
                } catch (e: Exception) {
                    NotificationCategory.GENERAL
                }
            } ?: NotificationCategory.GENERAL
            val priority = data["priority"]?.let {
                try {
                    NotificationPriority.valueOf(it)
                } catch (e: Exception) {
                    NotificationPriority.MEDIUM
                }
            } ?: NotificationPriority.MEDIUM
            val imageUrl = data["imageUrl"]
            val deepLink = data["deepLink"]

            // Create Firebase notification
            val firebaseNotification = FirebaseNotification(
                id = notificationId,
                userId = userId,
                type = type,
                category = category,
                title = title,
                body = body,
                imageUrl = imageUrl,
                isRead = false,
                priority = priority,
                data = data.toMap(),
                deepLink = deepLink,
                createdAt = Timestamp.now(),
                isSystemNotification = true
            )

            // Save to Firestore and show system notification
            serviceScope.launch {
                try {
                    // Save to Firestore
                    val saveResult = notificationRepository.createNotification(firebaseNotification)
                    if (saveResult.isSuccess) {
                        Log.d(TAG, "Notification saved to Firestore: $notificationId")

                        // Show system notification
                        notificationManagerService.showNotification(firebaseNotification)
                        Log.d(TAG, "System notification displayed: $notificationId")
                    } else {
                        Log.e(
                            TAG,
                            "Failed to save notification to Firestore: ${saveResult.exceptionOrNull()}"
                        )
                        // Still show system notification even if Firestore save fails
                        notificationManagerService.showNotification(firebaseNotification)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling data message", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing FCM data message", e)
        }
    }

    private fun handleNotificationMessage(
        notification: RemoteMessage.Notification,
        data: Map<String, String>,
        userId: String
    ) {
        try {
            // Extract data from FCM notification payload
            val notificationId = data["notificationId"] ?: generateNotificationId()
            val type = data["type"]?.let {
                try {
                    NotificationType.valueOf(it)
                } catch (e: Exception) {
                    NotificationType.SYSTEM
                }
            } ?: NotificationType.SYSTEM
            val category = data["category"]?.let {
                try {
                    NotificationCategory.valueOf(it)
                } catch (e: Exception) {
                    NotificationCategory.GENERAL
                }
            } ?: NotificationCategory.GENERAL
            val priority = data["priority"]?.let {
                try {
                    NotificationPriority.valueOf(it)
                } catch (e: Exception) {
                    NotificationPriority.MEDIUM
                }
            } ?: NotificationPriority.MEDIUM

            // Create Firebase notification from FCM notification
            val firebaseNotification = FirebaseNotification(
                id = notificationId,
                userId = userId,
                type = type,
                category = category,
                title = notification.title ?: "New Notification",
                body = notification.body ?: "",
                imageUrl = notification.imageUrl?.toString(),
                isRead = false,
                priority = priority,
                data = data.toMap(),
                deepLink = data["deepLink"],
                createdAt = Timestamp.now(),
                isSystemNotification = true
            )

            // Save to Firestore and show system notification
            serviceScope.launch {
                try {
                    // Save to Firestore
                    val saveResult = notificationRepository.createNotification(firebaseNotification)
                    if (saveResult.isSuccess) {
                        Log.d(TAG, "Notification saved to Firestore: $notificationId")
                    } else {
                        Log.e(
                            TAG,
                            "Failed to save notification to Firestore: ${saveResult.exceptionOrNull()}"
                        )
                    }

                    // Show system notification (FCM might have already shown it, but we ensure it's shown with our styling)
                    notificationManagerService.showNotification(firebaseNotification)
                    Log.d(TAG, "System notification displayed: $notificationId")

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling notification message", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing FCM notification message", e)
        }
    }

    private fun generateNotificationId(): String {
        return "fcm_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up coroutine scope
        serviceScope.coroutineContext.cancel()
    }
}