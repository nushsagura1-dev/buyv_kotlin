package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.repository.UserProfileRepository

class GetUserProfileUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<UserProfile> =
        userProfileRepository.getUserProfile(uid)
}
