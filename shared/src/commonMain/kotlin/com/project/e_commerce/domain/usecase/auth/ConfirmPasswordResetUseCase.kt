package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * Use case to confirm a password reset with a token and new password.
 */
class ConfirmPasswordResetUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> {
        if (token.isBlank()) {
            return Result.Error(ApiError.ValidationError("Reset token cannot be empty"))
        }
        if (newPassword.isBlank()) {
            return Result.Error(ApiError.ValidationError("New password cannot be empty"))
        }
        if (newPassword.length < 6) {
            return Result.Error(ApiError.ValidationError("Password must be at least 6 characters"))
        }
        return try {
            authRepository.confirmPasswordReset(token, newPassword)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to confirm password reset"))
        }
    }
}
