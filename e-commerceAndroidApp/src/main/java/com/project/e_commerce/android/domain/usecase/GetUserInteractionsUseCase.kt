package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.android.domain.model.UserPost
import com.project.e_commerce.android.domain.model.UserProduct
import com.project.e_commerce.android.domain.repository.UserProfileRepository

class GetUserLikedPostsUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<List<UserPost>> =
        userProfileRepository.getUserLikedPosts(uid)
}

class GetUserLikedProductsUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<List<UserProduct>> =
        userProfileRepository.getUserLikedProducts(uid)
}

class GetUserBookmarkedPostsUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<List<UserPost>> =
        userProfileRepository.getUserBookmarkedPosts(uid)
}

class GetUserBookmarkedProductsUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<List<UserProduct>> =
        userProfileRepository.getUserBookmarkedProducts(uid)
}
