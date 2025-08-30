package com.project.e_commerce.android.presentation.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.project.e_commerce.android.MainActivity
import com.project.e_commerce.android.R
import com.project.e_commerce.android.domain.model.FirebaseNotification
import com.project.e_commerce.android.domain.model.NotificationPriority
import com.project.e_commerce.android.domain.model.NotificationType

class NotificationManagerService(private val context: Context) {

    companion object {
        private const val TAG = "NotificationManagerService"
        private const val CHANNEL_ID_HIGH = "ecommerce_notifications_high"
        private const val CHANNEL_ID_MEDIUM = "ecommerce_notifications_medium"
        private const val CHANNEL_ID_LOW = "ecommerce_notifications_low"
        private const val NOTIFICATION_REQUEST_CODE = 101
    }

    init {
        createNotificationChannels()
    }

    fun showNotification(notification: FirebaseNotification) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent for notification tap
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            notification.deepLink?.let { putExtra("deepLink", it) }
            putExtra("notificationType", notification.type.name)
            putExtra("notificationId", notification.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Choose channel based on priority
        val channelId = when (notification.priority) {
            NotificationPriority.HIGH -> CHANNEL_ID_HIGH
            NotificationPriority.MEDIUM -> CHANNEL_ID_MEDIUM
            NotificationPriority.LOW -> CHANNEL_ID_LOW
        }

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getNotificationIcon(notification.type))
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(getNotificationPriority(notification.priority))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))

        // Add sender image for user-generated notifications
        if (!notification.isSystemNotification && notification.senderImageUrl != null) {
            // You can implement image loading here if needed
            // For now, using default profile icon
        }

        // Generate unique notification ID
        val notificationId = if (notification.id.isNotEmpty()) {
            notification.id.hashCode()
        } else {
            System.currentTimeMillis().toInt()
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "System notification shown with ID: $notificationId")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High priority channel (Orders, Payments, Security)
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "Important Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important notifications like orders, payments, and security alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Medium priority channel (Social, Promotions)
            val mediumChannel = NotificationChannel(
                CHANNEL_ID_MEDIUM,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications like social interactions and promotions"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Low priority channel (App updates, General)
            val lowChannel = NotificationChannel(
                CHANNEL_ID_LOW,
                "Low Priority Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Low priority notifications like app updates"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(highChannel)
            notificationManager.createNotificationChannel(mediumChannel)
            notificationManager.createNotificationChannel(lowChannel)

            Log.d(TAG, "Notification channels created")
        }
    }

    private fun getNotificationIcon(type: NotificationType): Int {
        return when (type) {
            // E-commerce & Orders
            NotificationType.ORDER_CONFIRMATION,
            NotificationType.ORDER_SHIPPED,
            NotificationType.ORDER_DELIVERED,
            NotificationType.ORDER_CANCELLED -> R.drawable.ic_cart

            NotificationType.PAYMENT_SUCCESS,
            NotificationType.PAYMENT_FAILED,
            NotificationType.REFUND_PROCESSED -> R.drawable.ic_cart

            // Social Interactions
            NotificationType.NEW_FOLLOWER -> R.drawable.ic_profile
            NotificationType.NEW_LIKE -> R.drawable.ic_love
            NotificationType.NEW_COMMENT,
            NotificationType.COMMENT_REPLY,
            NotificationType.MENTION -> R.drawable.ic_comment

            // Content & Reels
            NotificationType.REEL_APPROVED,
            NotificationType.CONTENT_MILESTONE,
            NotificationType.TRENDING_REEL,
            NotificationType.UPLOAD_COMPLETE -> R.drawable.ic_video_camera

            // Default
            else -> R.drawable.ic_notification
        }
    }

    private fun getNotificationPriority(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
        }
    }
}