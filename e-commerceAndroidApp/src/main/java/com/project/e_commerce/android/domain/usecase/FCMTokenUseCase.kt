package com.project.e_commerce.android.domain.usecase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.project.e_commerce.android.domain.repository.NotificationRepository
import kotlinx.coroutines.tasks.await

class FCMTokenUseCase(
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "FCMTokenUseCase"
    }

    /**
     * Registers the current FCM token for the authenticated user
     */
    suspend fun registerCurrentUserToken(): Result<String> = runCatching {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("User not authenticated")

        Log.d(TAG, "Registering FCM token for user: ${currentUser.uid}")

        // Get current FCM token
        val token = FirebaseMessaging.getInstance().token.await()
        Log.d(TAG, "Retrieved FCM token: $token")

        // Update token in repository
        val result = notificationRepository.updateFCMToken(currentUser.uid, token)

        if (result.isSuccess) {
            Log.d(TAG, "✅ FCM token registered successfully")
            token
        } else {
            Log.e(TAG, "❌ Failed to register FCM token: ${result.exceptionOrNull()}")
            throw result.exceptionOrNull() ?: Exception("Unknown error")
        }
    }

    /**
     * Subscribes to a topic for receiving targeted notifications
     */
    suspend fun subscribeToTopic(topic: String): Result<Unit> = runCatching {
        Log.d(TAG, "Subscribing to topic: $topic")
        FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
        Log.d(TAG, "✅ Successfully subscribed to topic: $topic")
    }

    /**
     * Unsubscribes from a topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Result<Unit> = runCatching {
        Log.d(TAG, "Unsubscribing from topic: $topic")
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
        Log.d(TAG, "✅ Successfully unsubscribed from topic: $topic")
    }

    /**
     * Gets the stored FCM token for a user
     */
    suspend fun getStoredToken(userId: String): Result<String?> {
        return notificationRepository.getFCMToken(userId)
    }

    /**
     * Refreshes the FCM token and updates it in the repository
     */
    suspend fun refreshToken(): Result<String> = runCatching {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("User not authenticated")

        Log.d(TAG, "Refreshing FCM token for user: ${currentUser.uid}")

        // Delete current token and get a new one
        FirebaseMessaging.getInstance().deleteToken().await()
        val newToken = FirebaseMessaging.getInstance().token.await()

        Log.d(TAG, "New FCM token retrieved: $newToken")

        // Update token in repository
        val result = notificationRepository.updateFCMToken(currentUser.uid, newToken)

        if (result.isSuccess) {
            Log.d(TAG, "✅ FCM token refreshed successfully")
            newToken
        } else {
            Log.e(TAG, "❌ Failed to refresh FCM token: ${result.exceptionOrNull()}")
            throw result.exceptionOrNull() ?: Exception("Unknown error")
        }
    }
}