package com.project.e_commerce.data.remote.api

import com.project.e_commerce.data.remote.dto.AuthResponseDto
import com.project.e_commerce.data.remote.dto.GoogleSignInRequestDto
import com.project.e_commerce.data.remote.dto.LoginRequestDto
import com.project.e_commerce.data.remote.dto.MessageResponseDto
import com.project.e_commerce.data.remote.dto.PasswordResetConfirmDto
import com.project.e_commerce.data.remote.dto.PasswordResetRequestDto
import com.project.e_commerce.data.remote.dto.PasswordResetResponseDto
import com.project.e_commerce.data.remote.dto.UserCreateDto
import com.project.e_commerce.data.remote.dto.UserDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Authentication API service
 * Handles user authentication endpoints
 * 
 * @param publicClient Client HTTP pour les endpoints publics (login, register)
 * @param authenticatedClient Client HTTP pour les endpoints authentifiés (getCurrentUser, logout)
 */
class AuthApiService(
    private val publicClient: HttpClient,
    private val authenticatedClient: HttpClient
) {
    
    /**
     * Register a new user
     * POST /auth/register
     */
    suspend fun register(userData: UserCreateDto): AuthResponseDto {
        val response: HttpResponse = publicClient.post("auth/register") {
            setBody(userData)
        }
        if (response.status.value >= 400) {
            val errorBody = try { response.bodyAsText() } catch (_: Exception) { "" }
            throw Exception("Registration failed (${response.status.value}): $errorBody")
        }
        return response.body()
    }
    
    /**
     * Login user
     * POST /auth/login
     */
    suspend fun login(credentials: LoginRequestDto): AuthResponseDto {
        val response: HttpResponse = publicClient.post("auth/login") {
            setBody(credentials)
        }
        // Check HTTP status before deserializing to avoid confusing JsonConvertException
        if (response.status.value == 401) {
            throw Exception("Invalid email or password")
        }
        if (response.status.value >= 400) {
            val errorBody = try { response.bodyAsText() } catch (_: Exception) { "" }
            throw Exception("Login failed (${response.status.value}): $errorBody")
        }
        return response.body()
    }
    
    /**
     * Get current authenticated user
     * GET /auth/me
     */
    suspend fun getCurrentUser(): UserDto {
        return authenticatedClient.get("auth/me").body()
    }
    
    /**
     * Refresh access token
     * POST /auth/refresh
     */
    suspend fun refreshToken(refreshToken: String): AuthResponseDto {
        return publicClient.post("auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("refreshToken" to refreshToken))
        }.body()
    }
    
    /**
     * Logout user (optional - for server-side token invalidation)
     * POST /auth/logout
     */
    suspend fun logout(): MessageResponseDto {
        return authenticatedClient.post("auth/logout").body()
    }
    
    /**
     * Request password reset
     * POST /auth/request-password-reset
     */
    suspend fun requestPasswordReset(email: String): PasswordResetResponseDto {
        return publicClient.post("auth/request-password-reset") {
            setBody(PasswordResetRequestDto(email))
        }.body()
    }
    
    /**
     * Confirm password reset with token and new password
     * POST /auth/confirm-password-reset
     */
    suspend fun confirmPasswordReset(token: String, newPassword: String): MessageResponseDto {
        return publicClient.post("auth/confirm-password-reset") {
            setBody(PasswordResetConfirmDto(token, newPassword))
        }.body()
    }

    /**
     * Google Sign-In — authenticate or register via Google ID token
     * POST /auth/google-signin
     */
    suspend fun googleSignIn(idToken: String): AuthResponseDto {
        return publicClient.post("auth/google-signin") {
            setBody(GoogleSignInRequestDto(idToken))
        }.body()
    }
}
