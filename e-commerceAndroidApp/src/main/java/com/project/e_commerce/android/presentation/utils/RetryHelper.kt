package com.project.e_commerce.android.presentation.utils

import android.util.Log
import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import kotlinx.coroutines.delay

/**
 * Utilitaire pour gérer le retry automatique sur les erreurs réseau.
 * 
 * Usage dans ViewModels:
 * ```kotlin
 * viewModelScope.launch {
 *     val result = retryOnError(maxRetries = 3) {
 *         apiService.getData()
 *     }
 *     when (result) {
 *         is Result.Success -> { /* handle success */ }
 *         is Result.Error -> { /* handle error */ }
 *     }
 * }
 * ```
 */
object RetryHelper {
    
    private const val TAG = "RetryHelper"
    
    /**
     * Retry automatique avec exponential backoff.
     * 
     * @param maxRetries Nombre maximum de tentatives (défaut: 3)
     * @param initialDelayMs Délai initial en millisecondes (défaut: 1000ms)
     * @param maxDelayMs Délai maximum en millisecondes (défaut: 10000ms)
     * @param factor Facteur d'augmentation du délai (défaut: 2.0)
     * @param block Bloc de code à exécuter
     * @return Result<T> Le résultat après toutes les tentatives
     */
    suspend fun <T> retryOnError(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        maxDelayMs: Long = 10000L,
        factor: Double = 2.0,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelayMs
        
        repeat(maxRetries) { attempt ->
            val result = try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "Attempt ${attempt + 1}/$maxRetries failed with exception", e)
                Result.Error(ApiError.Unknown(e.message ?: "Unknown error"))
            }
            
            // Si succès ou dernier essai, retourner le résultat
            when {
                result is Result.Success -> {
                    if (attempt > 0) {
                        Log.i(TAG, "✅ Success after ${attempt + 1} attempts")
                    }
                    return result
                }
                attempt == maxRetries - 1 -> {
                    Log.e(TAG, "❌ Failed after $maxRetries attempts")
                    return result
                }
                else -> {
                    // Retry avec exponential backoff
                    Log.w(TAG, "⚠️ Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms...")
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                }
            }
        }
        
        // Fallback (ne devrait jamais arriver)
        return Result.Error(ApiError.NetworkError)
    }
    
    /**
     * Vérifie si une erreur est "retriable" (temporaire).
     * 
     * @param error L'erreur à vérifier
     * @return true si l'erreur peut être retry
     */
    fun isRetriableError(error: ApiError): Boolean {
        return when (error) {
            is ApiError.NetworkError -> true
            is ApiError.ServerError -> true // 5xx errors
            is ApiError.Unknown -> true
            else -> false // Unauthorized, Forbidden, NotFound, ValidationError ne sont pas retriables
        }
    }
    
    /**
     * Retry conditionnel - retry uniquement si l'erreur est retriable.
     * 
     * @param maxRetries Nombre maximum de tentatives
     * @param block Bloc de code à exécuter
     * @return Result<T> Le résultat après toutes les tentatives
     */
    suspend fun <T> retryOnNetworkError(
        maxRetries: Int = 3,
        block: suspend () -> Result<T>
    ): Result<T> {
        return retryOnError(maxRetries = maxRetries) {
            val result = block()
            
            // Si erreur non-retriable, retourner immédiatement
            if (result is Result.Error && !isRetriableError(result.error)) {
                Log.d(TAG, "Non-retriable error detected, skipping retry")
                return@retryOnError result
            }
            
            result
        }
    }
}

