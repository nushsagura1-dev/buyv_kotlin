package com.project.e_commerce.android.domain.usecase

import android.content.Context
import com.project.e_commerce.android.data.repository.AuthRepository

class GoogleSignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String) =
        authRepository.signInWithGoogle(idToken)
}