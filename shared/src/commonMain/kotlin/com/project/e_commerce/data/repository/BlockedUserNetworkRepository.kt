package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.BlockedUserApiService
import com.project.e_commerce.data.remote.dto.BlockedUserDto
import com.project.e_commerce.domain.model.BlockedUser
import com.project.e_commerce.domain.repository.BlockedUserRepository
import kotlinx.datetime.Clock

/**
 * Network implementation of BlockedUserRepository.
 */
class BlockedUserNetworkRepository(
    private val blockedUserApiService: BlockedUserApiService
) : BlockedUserRepository {

    override suspend fun getBlockedUsers(): List<BlockedUser> {
        return blockedUserApiService.getBlockedUsers().map { it.toDomain() }
    }

    override suspend fun blockUser(userId: String): BlockedUser {
        return blockedUserApiService.blockUser(userId).toDomain()
    }

    override suspend fun unblockUser(userUid: String) {
        blockedUserApiService.unblockUser(userUid)
    }

    override suspend fun isUserBlocked(userUid: String): Boolean {
        return blockedUserApiService.checkBlockStatus(userUid).isBlocked
    }

    private fun BlockedUserDto.toDomain(): BlockedUser {
        return BlockedUser(
            id = id,
            blockedUid = blockedUid,
            blockedUsername = blockedUsername,
            blockedDisplayName = blockedDisplayName,
            blockedProfileImage = blockedProfileImage,
            createdAt = parseTimestamp(createdAt)
        )
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            kotlinx.datetime.Instant.parse(timestamp).toEpochMilliseconds()
        } catch (e: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }
    }
}
