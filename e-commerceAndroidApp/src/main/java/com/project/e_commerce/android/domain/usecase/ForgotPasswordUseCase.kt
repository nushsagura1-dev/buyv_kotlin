package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.domain.repository.AuthRepository

class ForgotPasswordUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String) = repo.forgotPassword(email)
}
