package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.UserRepository

class GetUserProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: String): Result<UserProfile> {
        return repository.getUserProfile(userId)
    }
}
