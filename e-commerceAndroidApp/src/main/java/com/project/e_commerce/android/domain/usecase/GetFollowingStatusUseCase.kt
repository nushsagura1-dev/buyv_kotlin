package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.model.FollowingStatus
import com.project.e_commerce.android.domain.repository.FollowingRepository

class GetFollowingStatusUseCase(
    private val followingRepository: FollowingRepository
) {
    suspend operator fun invoke(followerId: String, followedId: String): FollowingStatus =
        followingRepository.getFollowingStatus(followerId, followedId)
}
