package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * Use case for sending password reset email
 */
class SendPasswordResetUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }
}
