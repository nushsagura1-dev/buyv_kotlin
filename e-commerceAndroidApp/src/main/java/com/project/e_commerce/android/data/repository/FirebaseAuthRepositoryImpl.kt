package com.project.e_commerce.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.model.User
import com.project.e_commerce.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await


class FirebaseAuthRepositoryImpl(private val auth: FirebaseAuth) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser

    init {
        auth.currentUser?.let {
            _currentUser.value = User(it.uid, it.email)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user?.let { User(it.uid, it.email) }
        _currentUser.value = user
        Result.success(user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun register(email: String, password: String): Result<User> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user?.let { User(it.uid, it.email) }
        _currentUser.value = user
        Result.success(user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun forgotPassword(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}
