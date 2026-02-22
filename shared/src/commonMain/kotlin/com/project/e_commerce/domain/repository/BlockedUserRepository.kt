package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.BlockedUser

/**
 * Repository interface for blocked user operations.
 */
interface BlockedUserRepository {
    /** Get list of users blocked by the current user. */
    suspend fun getBlockedUsers(): List<BlockedUser>

    /** Block a user. Returns the created BlockedUser entry. */
    suspend fun blockUser(userId: String): BlockedUser

    /** Unblock a user. */
    suspend fun unblockUser(userUid: String)

    /** Check if a user is blocked. */
    suspend fun isUserBlocked(userUid: String): Boolean
}
