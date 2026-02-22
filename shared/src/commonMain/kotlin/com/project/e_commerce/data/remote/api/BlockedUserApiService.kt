package com.project.e_commerce.data.remote.api

import com.project.e_commerce.data.remote.dto.BlockStatusDto
import com.project.e_commerce.data.remote.dto.BlockUserRequestDto
import com.project.e_commerce.data.remote.dto.BlockedUserDto
import com.project.e_commerce.data.remote.dto.MessageResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Blocked Users API Service.
 * Handles block/unblock operations on users.
 *
 * Endpoints:
 *  - GET    /api/users/me/blocked               → list blocked users
 *  - POST   /api/users/me/blocked               → block a user
 *  - DELETE  /api/users/me/blocked/{user_uid}    → unblock a user
 *  - GET    /api/users/me/blocked/{user_uid}/status → check block status
 */
class BlockedUserApiService(private val httpClient: HttpClient) {

    private val baseUrl = "api/users/me/blocked"

    /**
     * Get list of users blocked by the current user.
     */
    suspend fun getBlockedUsers(): List<BlockedUserDto> {
        return httpClient.get(baseUrl).body()
    }

    /**
     * Block a user by their UID.
     */
    suspend fun blockUser(userId: String): BlockedUserDto {
        return httpClient.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(BlockUserRequestDto(userId = userId))
        }.body()
    }

    /**
     * Unblock a user by their UID.
     */
    suspend fun unblockUser(userUid: String): MessageResponseDto {
        return httpClient.delete("$baseUrl/$userUid").body()
    }

    /**
     * Check if a user is blocked by the current user.
     */
    suspend fun checkBlockStatus(userUid: String): BlockStatusDto {
        return httpClient.get("$baseUrl/$userUid/status").body()
    }
}
