package com.project.e_commerce.android.domain.usecase

import com.project.e_commerce.domain.repository.AuthRepository


class LoginUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = repo.login(email, password)
}
