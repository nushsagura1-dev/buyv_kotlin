package com.project.e_commerce.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.e_commerce.android.domain.model.FirebaseNotification
import com.project.e_commerce.android.domain.model.NotificationCategory
import com.project.e_commerce.android.domain.model.NotificationType
import com.project.e_commerce.android.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : NotificationRepository {

    companion object {
        private const val TAG = "FirebaseNotificationRepo"
        private const val USERS_COLLECTION = "users"
        private const val NOTIFICATIONS_COLLECTION = "notifications"
        private const val FCM_TOKENS_COLLECTION = "fcm_tokens"
    }

    override fun getNotifications(userId: String): Flow<List<FirebaseNotification>> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications", error)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val notifications = querySnapshot.toObjects(FirebaseNotification::class.java)
                    Log.d(TAG, "Retrieved ${notifications.size} notifications for user: $userId")
                    trySend(notifications)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getNotificationsByCategory(
        userId: String,
        category: NotificationCategory
    ): Flow<List<FirebaseNotification>> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("category", category.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications by category", error)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val notifications = querySnapshot.toObjects(FirebaseNotification::class.java)
                    Log.d(
                        TAG,
                        "Retrieved ${notifications.size} notifications for category: $category"
                    )
                    trySend(notifications)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getNotificationsByType(
        userId: String,
        type: NotificationType
    ): Flow<List<FirebaseNotification>> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("type", type.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications by type", error)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val notifications = querySnapshot.toObjects(FirebaseNotification::class.java)
                    Log.d(TAG, "Retrieved ${notifications.size} notifications for type: $type")
                    trySend(notifications)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getUnreadNotifications(userId: String): Flow<List<FirebaseNotification>> =
        callbackFlow {
            val listener = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("isRead", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to unread notifications", error)
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        val notifications =
                            querySnapshot.toObjects(FirebaseNotification::class.java)
                        Log.d(TAG, "Retrieved ${notifications.size} unread notifications")
                        trySend(notifications)
                    }
                }

            awaitClose { listener.remove() }
        }

    override suspend fun getNotificationById(
        userId: String,
        notificationId: String
    ): Result<FirebaseNotification?> = try {
        val document = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .document(notificationId)
            .get()
            .await()

        val notification = document.toObject(FirebaseNotification::class.java)
        Log.d(TAG, "Retrieved notification: $notificationId")
        Result.success(notification)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting notification by ID", e)
        Result.failure(e)
    }

    override suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .document(notificationId)
            .update("isRead", true)
            .await()

        Log.d(TAG, "Marked notification as read: $notificationId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error marking notification as read", e)
        Result.failure(e)
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> = try {
        val batch = firestore.batch()

        val notifications = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        notifications.documents.forEach { document ->
            batch.update(document.reference, "isRead", true)
        }

        batch.commit().await()
        Log.d(TAG, "Marked all notifications as read for user: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error marking all notifications as read", e)
        Result.failure(e)
    }

    override suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> =
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .delete()
                .await()

            Log.d(TAG, "Deleted notification: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            Result.failure(e)
        }

    override suspend fun deleteAllNotifications(userId: String): Result<Unit> = try {
        val batch = firestore.batch()

        val notifications = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .get()
            .await()

        notifications.documents.forEach { document ->
            batch.delete(document.reference)
        }

        batch.commit().await()
        Log.d(TAG, "Deleted all notifications for user: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting all notifications", e)
        Result.failure(e)
    }

    override suspend fun createNotification(notification: FirebaseNotification): Result<Unit> =
        try {
            val notificationWithId = if (notification.id.isEmpty()) {
                notification.copy(id = firestore.collection("temp").document().id)
            } else {
                notification
            }

            firestore.collection(USERS_COLLECTION)
                .document(notification.userId)
                .collection(NOTIFICATIONS_COLLECTION)
                .document(notificationWithId.id)
                .set(notificationWithId)
                .await()

            Log.d(
                TAG,
                "Created notification: ${notificationWithId.id} for user: ${notification.userId}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            Result.failure(e)
        }

    override suspend fun createBulkNotifications(notifications: List<FirebaseNotification>): Result<Unit> =
        try {
            val batch = firestore.batch()

            notifications.forEach { notification ->
                val notificationWithId = if (notification.id.isEmpty()) {
                    notification.copy(id = firestore.collection("temp").document().id)
                } else {
                    notification
                }

                val docRef = firestore.collection(USERS_COLLECTION)
                    .document(notification.userId)
                    .collection(NOTIFICATIONS_COLLECTION)
                    .document(notificationWithId.id)

                batch.set(docRef, notificationWithId)
            }

            batch.commit().await()
            Log.d(TAG, "Created ${notifications.size} bulk notifications")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating bulk notifications", e)
            Result.failure(e)
        }

    override fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to unread count", error)
                    return@addSnapshotListener
                }

                val count = snapshot?.size() ?: 0
                Log.d(TAG, "Unread notifications count: $count")
                trySend(count)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateFCMToken(userId: String, token: String): Result<Unit> = try {
        firestore.collection(FCM_TOKENS_COLLECTION)
            .document(userId)
            .set(mapOf("token" to token, "updatedAt" to com.google.firebase.Timestamp.now()))
            .await()

        Log.d(TAG, "Updated FCM token for user: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating FCM token", e)
        Result.failure(e)
    }

    override suspend fun getFCMToken(userId: String): Result<String?> = try {
        val document = firestore.collection(FCM_TOKENS_COLLECTION)
            .document(userId)
            .get()
            .await()

        val token = document.getString("token")
        Log.d(TAG, "Retrieved FCM token for user: $userId")
        Result.success(token)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting FCM token", e)
        Result.failure(e)
    }
}