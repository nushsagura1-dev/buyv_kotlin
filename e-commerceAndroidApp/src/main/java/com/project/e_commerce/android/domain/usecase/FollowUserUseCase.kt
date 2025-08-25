package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.repository.FollowingRepository

class FollowUserUseCase(
    private val followingRepository: FollowingRepository
) {
    suspend operator fun invoke(followerId: String, followedId: String) =
        followingRepository.followUser(followerId, followedId)
}
