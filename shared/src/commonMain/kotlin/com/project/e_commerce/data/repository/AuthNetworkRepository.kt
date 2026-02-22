package com.project.e_commerce.data.repository

import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.data.remote.api.AuthApiService
import com.project.e_commerce.data.remote.api.UserApiService
import com.project.e_commerce.data.remote.dto.LoginRequestDto
import com.project.e_commerce.data.remote.dto.UserCreateDto
import com.project.e_commerce.data.remote.dto.UserUpdateDto
import com.project.e_commerce.data.remote.mapper.toDomain
import com.project.e_commerce.data.validator.InputValidator
import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository
import io.ktor.client.plugins.*
import io.ktor.utils.io.errors.*
import kotlinx.datetime.Clock

/**
 * Network implementation of AuthRepository using FastAPI backend
 */
class AuthNetworkRepository(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager,
    private val userApi: UserApiService
) : AuthRepository {
    
    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        return try {
            // Validate inputs
            val emailValidation = InputValidator.validateEmail(email)
            if (!emailValidation.isValid) {
                return Result.Error(ApiError.ValidationError(emailValidation.errors.firstOrNull() ?: "Invalid email"))
            }
            
            val passwordValidation = InputValidator.validatePassword(password)
            if (!passwordValidation.isValid) {
                return Result.Error(ApiError.ValidationError(passwordValidation.errors.firstOrNull() ?: "Invalid password"))
            }
            
            // Call backend API
            val response = authApi.login(LoginRequestDto(email, password))
            
            // Store tokens
            tokenManager.saveAccessToken(response.accessToken)
            response.refreshToken?.let { tokenManager.saveRefreshToken(it) }
            
            // Calculate and store token expiration
            val expirationTime = (Clock.System.now().toEpochMilliseconds() / 1000) + response.expiresIn
            tokenManager.saveTokenExpiration(expirationTime)
            
            // Return user profile
            Result.Success(response.user.toDomain())
            
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<UserProfile> {
        return try {
            // Validate all inputs
            val emailValidation = InputValidator.validateEmail(email)
            if (!emailValidation.isValid) {
                return Result.Error(ApiError.ValidationError(emailValidation.errors.firstOrNull() ?: "Invalid email"))
            }
            
            val passwordValidation = InputValidator.validatePassword(password)
            if (!passwordValidation.isValid) {
                return Result.Error(ApiError.ValidationError(passwordValidation.errors.firstOrNull() ?: "Invalid password"))
            }
            
            // Generate username from displayName or email
            val username = displayName.replace("\\s+".toRegex(), "_").lowercase().take(50)
            
            // Create user data with only 4 fields that UserCreateDto accepts
            val userData = UserCreateDto(
                email = email,
                username = username,
                password = password,
                displayName = displayName
            )
            
            // Call backend API
            val response = authApi.register(userData)
            
            // Store tokens
            tokenManager.saveAccessToken(response.accessToken)
            response.refreshToken?.let { tokenManager.saveRefreshToken(it) }
            
            // Calculate and store token expiration
            val expirationTime = (Clock.System.now().toEpochMilliseconds() / 1000) + response.expiresIn
            tokenManager.saveTokenExpiration(expirationTime)
            
            // Return user profile
            Result.Success(response.user.toDomain())
            
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            // Call backend to invalidate token (optional)
            authApi.logout()
            
            // Clear local tokens
            tokenManager.clearTokens()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            // Even if backend call fails, clear local tokens
            tokenManager.clearTokens()
            Result.Success(Unit)
        }
    }
    
    override suspend fun getCurrentUser(): Result<UserProfile> {
        return try {
            // Check if token exists and is valid
            if (tokenManager.isTokenExpired()) {
                return Result.Error(ApiError.Unauthorized)
            }
            
            val accessToken = tokenManager.getAccessToken()
            if (accessToken == null) {
                return Result.Error(ApiError.Unauthorized)
            }
            
            // Get current user from backend
            val userDto = authApi.getCurrentUser()
            Result.Success(userDto.toDomain())
            
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 401) {
                // Token invalid, clear tokens
                tokenManager.clearTokens()
                Result.Error(ApiError.Unauthorized)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun isUserSignedIn(): Boolean {
        val token = tokenManager.getAccessToken()
        return token != null && !tokenManager.isTokenExpired()
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            // Validate email
            val emailValidation = InputValidator.validateEmail(email)
            if (!emailValidation.isValid) {
                return Result.Error(ApiError.ValidationError(emailValidation.errors.firstOrNull() ?: "Invalid email"))
            }
            
            // Call backend API
            val response = authApi.requestPasswordReset(email)
            
            Result.Success(Unit)
            
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to send password reset email"))
        }
    }
    
    override suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit> {
        return try {
            authApi.confirmPasswordReset(token, newPassword)
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to confirm password reset"))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> {
        if (idToken.isBlank()) {
            return Result.Error(ApiError.ValidationError("Google ID token is required"))
        }
        return try {
            // Call backend Google Sign-In endpoint
            val response = authApi.googleSignIn(idToken)
            
            // Store tokens (same pattern as signInWithEmail)
            tokenManager.saveAccessToken(response.accessToken)
            response.refreshToken?.let { tokenManager.saveRefreshToken(it) }
            
            // Calculate and store token expiration
            val expirationTime = (Clock.System.now().toEpochMilliseconds() / 1000) + response.expiresIn
            tokenManager.saveTokenExpiration(expirationTime)
            
            // Return user profile
            Result.Success(response.user.toDomain())
            
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Google Sign-In failed"))
        }
    }
    
    override suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            val updateDto = UserUpdateDto(
                displayName = userProfile.displayName.ifBlank { null },
                profileImageUrl = userProfile.profileImageUrl,
                bio = userProfile.bio.ifBlank { null }
            )
            userApi.updateUserProfile(userProfile.uid, updateDto)
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to update profile"))
        }
    }
    
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            userApi.deleteAccount()
            // Clear local tokens after successful account deletion
            tokenManager.clearTokens()
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Failed to delete account"))
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshAccessToken(): Result<String> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                tokenManager.clearTokens()
                return Result.Error(ApiError.Unauthorized)
            }
            
            val response = authApi.refreshToken(refreshToken)
            
            // Store new tokens
            tokenManager.saveAccessToken(response.accessToken)
            response.refreshToken?.let { tokenManager.saveRefreshToken(it) }
            
            // Calculate and store token expiration
            val expirationTime = (Clock.System.now().toEpochMilliseconds() / 1000) + response.expiresIn
            tokenManager.saveTokenExpiration(expirationTime)
            
            Result.Success(response.accessToken)
            
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.Error(ApiError.Unauthorized)
        }
    }
}
