package com.project.e_commerce.domain.repository

import com.project.e_commerce.android.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String): Result<User>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun logout()
    val currentUser: Flow<User?>
    suspend fun signInWithGoogle(idToken: String): com.project.e_commerce.domain.model.Result<com.project.e_commerce.domain.model.UserProfile> =
        throw NotImplementedError("signInWithGoogle not implemented")
}
