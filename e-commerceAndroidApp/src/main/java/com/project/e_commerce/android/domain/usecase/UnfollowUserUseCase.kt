package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.FollowingRepository

class UnfollowUserUseCase(
    private val followingRepository: FollowingRepository
) {
    suspend operator fun invoke(followerId: String, followedId: String) =
        followingRepository.unfollowUser(followerId, followedId)
}
