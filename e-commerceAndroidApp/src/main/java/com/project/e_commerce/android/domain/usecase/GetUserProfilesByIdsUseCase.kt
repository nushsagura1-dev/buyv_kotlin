package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.repository.UserProfileRepository

class GetUserProfilesByIdsUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(userIds: List<String>): List<UserProfile> {
        return userIds.mapNotNull { userId ->
            try {
                val result = userProfileRepository.getUserProfile(userId)
                if (result.isSuccess) {
                    result.getOrNull()
                } else {
                    null // Skip users that can't be loaded
                }
            } catch (e: Exception) {
                null // Skip users that can't be loaded
            }
        }
    }
}
