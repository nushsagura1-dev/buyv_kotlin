package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.FollowingRepository

class GetFollowersUseCase(
    private val followingRepository: FollowingRepository
) {
    suspend operator fun invoke(userId: String): List<String> {
        return followingRepository.getFollowers(userId)
    }
}
