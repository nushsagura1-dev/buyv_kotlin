package com.project.e_commerce.android.presentation.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.model.FirebaseNotification
import com.project.e_commerce.android.domain.model.NotificationCategory
import com.project.e_commerce.android.domain.usecase.GetNotificationsUseCase
import com.project.e_commerce.android.domain.usecase.GetUnreadCountUseCase
import com.project.e_commerce.android.domain.usecase.MarkAsReadUseCase
import com.project.e_commerce.android.domain.usecase.CreateNotificationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class NotificationState(
    val isLoading: Boolean = false,
    val notifications: List<FirebaseNotification> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null,
    val selectedCategory: NotificationCategory? = null
)

class NotificationViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val createNotificationUseCase: CreateNotificationUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationState())
    val state: StateFlow<NotificationState> = _state.asStateFlow()

    companion object {
        private const val TAG = "NotificationViewModel"
    }

    init {
        loadNotifications()
        loadUnreadCount()
    }

    fun loadNotifications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "User not authenticated"
            )
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                getNotificationsUseCase(currentUser.uid)
                    .catch { exception ->
                        Log.e(TAG, "Error loading notifications", exception)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Unknown error occurred"
                        )
                    }
                    .collect { notifications ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            notifications = notifications,
                            error = null
                        )
                        Log.d(TAG, "Loaded ${notifications.size} notifications")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadNotifications", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun loadUnreadCount() {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                getUnreadCountUseCase(currentUser.uid)
                    .catch { exception ->
                        Log.e(TAG, "Error loading unread count", exception)
                    }
                    .collect { count ->
                        _state.value = _state.value.copy(unreadCount = count)
                        Log.d(TAG, "Unread notifications count: $count")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadUnreadCount", e)
            }
        }
    }

    fun markAsRead(notificationId: String) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val result = markAsReadUseCase(currentUser.uid, notificationId)
                if (result.isSuccess) {
                    Log.d(TAG, "Marked notification as read: $notificationId")
                    // Update local state
                    val updatedNotifications = _state.value.notifications.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }
                    _state.value = _state.value.copy(notifications = updatedNotifications)
                } else {
                    Log.e(TAG, "Failed to mark notification as read: ${result.exceptionOrNull()}")
                    _state.value = _state.value.copy(
                        error = "Failed to mark notification as read"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun filterByCategory(category: NotificationCategory?) {
        _state.value = _state.value.copy(selectedCategory = category)
    }

    fun getFilteredNotifications(): List<FirebaseNotification> {
        val currentCategory = _state.value.selectedCategory
        return if (currentCategory != null) {
            _state.value.notifications.filter { it.category == currentCategory }
        } else {
            _state.value.notifications
        }
    }

    fun getTodayNotifications(): List<FirebaseNotification> {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000) // 24 hours ago

        return _state.value.notifications.filter { notification ->
            val notificationTime = notification.createdAt.seconds * 1000
            notificationTime >= oneDayAgo
        }
    }

    fun getYesterdayNotifications(): List<FirebaseNotification> {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000) // 24 hours ago
        val twoDaysAgo = now - (48 * 60 * 60 * 1000) // 48 hours ago

        return _state.value.notifications.filter { notification ->
            val notificationTime = notification.createdAt.seconds * 1000
            notificationTime in twoDaysAgo..oneDayAgo
        }
    }

    fun getOlderNotifications(): List<FirebaseNotification> {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (48 * 60 * 60 * 1000) // 48 hours ago

        return _state.value.notifications.filter { notification ->
            val notificationTime = notification.createdAt.seconds * 1000
            notificationTime < twoDaysAgo
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refresh() {
        loadNotifications()
        loadUnreadCount()
    }

    // Helper function to create a test notification (for development/testing)
    fun createTestNotification(
        title: String,
        body: String,
        type: com.project.e_commerce.android.domain.model.NotificationType
    ) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                val notification = FirebaseNotification(
                    userId = currentUser.uid,
                    type = type,
                    title = title,
                    body = body,
                    isRead = false,
                    createdAt = com.google.firebase.Timestamp.now()
                )

                val result = createNotificationUseCase(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Test notification created successfully")
                } else {
                    Log.e(TAG, "Failed to create test notification: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating test notification", e)
            }
        }
    }
}