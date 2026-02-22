package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.domain.repository.AuthRepository

class LogoutUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.logout()
}
