package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.model.UserPost
import com.project.e_commerce.android.domain.model.UserProduct
import com.project.e_commerce.android.domain.repository.UserProfileRepository

class GetUserPostsUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<List<UserPost>> =
        userProfileRepository.getUserPosts(uid)
}

class GetUserReelsUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<List<UserPost>> =
        userProfileRepository.getUserReels(uid)
}

class GetUserProductsUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<List<UserProduct>> =
        userProfileRepository.getUserProducts(uid)
}
