package com.project.e_commerce.android.data.api

import retrofit2.http.*

/**
 * Notifications API
 * Handles user notifications for orders, follows, likes, and comments
 */
interface NotificationsApi {
    
    /**
     * Get all notifications for current user
     * GET /notifications/?skip={skip}&limit={limit}&unread_only={unread}
     * 
     * @param token User authentication token (Bearer)
     * @param skip Pagination offset
     * @param limit Number of items per page
     * @param unreadOnly Filter to show only unread notifications
     * @return List of user notifications
     */
    @GET("notifications/")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("unread_only") unreadOnly: Boolean = false
    ): List<NotificationDto>
    
    /**
     * Create a test notification (admin/dev only)
     * POST /notifications/
     * 
     * @param token User authentication token (Bearer)
     * @param request Notification creation request
     * @return Created notification
     */
    @POST("notifications/")
    suspend fun createNotification(
        @Header("Authorization") token: String,
        @Body request: NotificationCreateRequest
    ): NotificationDto
    
    /**
     * Mark a notification as read
     * POST /notifications/{id}/read
     * 
     * @param token User authentication token (Bearer)
     * @param notificationId Notification ID to mark as read
     * @return Success message
     */
    @POST("notifications/{id}/read")
    suspend fun markAsRead(
        @Header("Authorization") token: String,
        @Path("id") notificationId: Int
    ): MessageResponseDto
}

/**
 * Notification DTO
 * 
 * @property id Unique notification ID
 * @property userId User ID who receives the notification
 * @property type Notification type (order, follow, like, comment)
 * @property title Notification title
 * @property message Notification message/body
 * @property isRead Whether the notification has been read
 * @property data Optional JSON data (e.g., order_id, post_id)
 * @property createdAt Creation timestamp
 */
data class NotificationDto(
    val id: Int,
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val data: Map<String, Any>? = null,
    val createdAt: String
)

/**
 * Notification creation request
 * 
 * @property userId Target user ID
 * @property type Notification type
 * @property title Notification title
 * @property message Notification message
 * @property data Optional metadata
 */
data class NotificationCreateRequest(
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val data: Map<String, Any>? = null
)

/**
 * Generic message response
 */
data class MessageResponseDto(
    val message: String,
    val detail: String? = null
)
