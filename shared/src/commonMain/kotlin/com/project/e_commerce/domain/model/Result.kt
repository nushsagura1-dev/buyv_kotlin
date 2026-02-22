package com.project.e_commerce.domain.model

import kotlinx.serialization.Serializable

/**
 * Classe générique pour encapsuler le résultat d'une opération.
 * 
 * Permet de gérer les succès et les échecs de manière cohérente.
 * Utilisation recommandée pour toutes les opérations pouvant échouer.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: ApiError) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw Exception(error.message)
        is Loading -> throw IllegalStateException("Result is still loading")
    }
}

/**
 * Types d'erreurs API standardisés.
 * 
 * Évite de leaker des informations sensibles dans les messages d'erreur.
 */
@Serializable
sealed class ApiError {
    abstract val message: String

    @Serializable
    data object NetworkError : ApiError() {
        override val message = "Network connection error"
    }

    @Serializable
    data object Unauthorized : ApiError() {
        override val message = "Unauthorized access"
    }

    @Serializable
    data object Forbidden : ApiError() {
        override val message = "Access forbidden"
    }

    @Serializable
    data object NotFound : ApiError() {
        override val message = "Resource not found"
    }

    @Serializable
    data object ServerError : ApiError() {
        override val message = "Server error occurred"
    }

    @Serializable
    data class ValidationError(override val message: String) : ApiError()

    @Serializable
    data class Unknown(override val message: String = "An error occurred") : ApiError()

    companion object {
        fun fromException(e: Exception): ApiError {
            return when {
                e.message?.contains("network", ignoreCase = true) == true -> NetworkError
                e.message?.contains("401", ignoreCase = true) == true -> Unauthorized
                e.message?.contains("404", ignoreCase = true) == true -> NotFound
                e.message?.contains("500", ignoreCase = true) == true -> ServerError
                else -> Unknown(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * Résultat de validation de champ.
 */
sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()

    val isValid: Boolean
        get() = this is Success
    
    // Helper property pour compatibilité avec code existant
    val errors: List<String>
        get() = if (this is Error) listOf(message) else emptyList()
}
