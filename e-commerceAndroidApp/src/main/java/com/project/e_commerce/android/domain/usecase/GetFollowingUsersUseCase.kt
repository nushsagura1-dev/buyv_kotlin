package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.FollowingRepository

class GetFollowingUsersUseCase(
    private val followingRepository: FollowingRepository
) {
    suspend operator fun invoke(userId: String): List<String> =
        followingRepository.getFollowing(userId)
}
