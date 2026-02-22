package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.repository.UserProfileRepository

class UpdateUserProfileUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(profile: UserProfile): Result<UserProfile> {
        return userProfileRepository.updateUserProfile(profile)
    }
}
