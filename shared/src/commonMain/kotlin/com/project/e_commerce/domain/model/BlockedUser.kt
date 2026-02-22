package com.project.e_commerce.domain.model

/**
 * Blocked user domain model.
 * Represents a user that the current user has blocked.
 */
data class BlockedUser(
    val id: Int,
    val blockedUid: String,
    val blockedUsername: String,
    val blockedDisplayName: String,
    val blockedProfileImage: String?,
    val createdAt: Long // Timestamp in milliseconds
)
