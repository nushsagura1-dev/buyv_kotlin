package com.project.e_commerce.android.data.repository
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<FirebaseUser>
    suspend fun signUp(email: String, password: String, displayName: String?): Result<FirebaseUser>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun currentUser(): FirebaseUser?
}
