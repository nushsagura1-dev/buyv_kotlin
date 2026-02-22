package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository

class GoogleSignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<UserProfile> =
        authRepository.signInWithGoogle(idToken)
}