package com.project.e_commerce.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

import com.project.e_commerce.getPlatform

/**
 * Configuration Ktor Client pour l'API Backend.
 * 
 * Configure les plugins nécessaires :
 * - Content Negotiation : Sérialisation/Désérialisation JSON
 * - Logging : Logs des requêtes (dev uniquement)
 * - Auth : Gestion automatique du token JWT
 * - Default Request : Configuration par défaut (headers, base URL)
 */
object KtorClientConfig {
    
    /**
     * URL de base de l'API Backend.
     * Utilise ApiEnvironment pour la configuration centralisée.
     * 
     * @see ApiEnvironment pour changer d'environnement (DEV / STAGING / PRODUCTION)
     */
    private val BASE_URL: String
        get() = ApiEnvironment.baseUrl
    
    /**
     * Configuration JSON pour la sérialisation.
     */
    private val jsonConfig = Json {
        ignoreUnknownKeys = true // Ignore les champs inconnus du backend
        isLenient = true // Accepte JSON non strict
        prettyPrint = false // Compact en production
        encodeDefaults = true // Encode les valeurs par défaut
    }
    
    /**
     * Crée une instance configurée de HttpClient.
     * 
     * @param tokenProvider Lambda pour récupérer le token JWT actuel
     * @param refreshTokenProvider Lambda pour récupérer le refresh token
     * @return HttpClient configuré
     */
    /**
     * Mutex to prevent concurrent token refresh calls.
     */
    private val refreshMutex = Mutex()

    fun create(
        tokenProvider: () -> String?,
        refreshTokenProvider: suspend (refreshToken: String) -> Pair<String, String>?
    ): HttpClient {
        return HttpClient(createPlatformEngine()) {
            // Timeouts
            install(HttpTimeout) {
                requestTimeoutMillis = 30000  // 30 secondes pour toutes les requêtes
                connectTimeoutMillis = 15000  // 15 secondes pour établir la connexion
                socketTimeoutMillis = 30000   // 30 secondes pour lire/écrire les données
            }
            
            // Content Negotiation - JSON
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            
            // Logging
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        if (ApiEnvironment.isDebugLoggingEnabled) {
                            println("KTOR: $message")
                        }
                    }
                }
                level = if (ApiEnvironment.isProduction) LogLevel.NONE else LogLevel.INFO
            }
            
            // Default Request Configuration (sans token - le token sera ajouté par l'intercepteur)
            defaultRequest {
                url(BASE_URL)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
        }.also { client ->
            // Intercepteur : ajoute le token, détecte 401, rafraîchit et réessaie
            client.plugin(HttpSend).intercept { request ->
                // 1. Ajouter le token actuel
                val token = tokenProvider()
                if (token != null) {
                    request.header(HttpHeaders.Authorization, "Bearer $token")
                    if (ApiEnvironment.isDebugLoggingEnabled) {
                        println("KTOR: Auth header added for: ${request.url}")
                    }
                } else if (ApiEnvironment.isDebugLoggingEnabled) {
                    println("KTOR: No token for: ${request.url}")
                }
                
                val originalCall = execute(request)
                
                // 2. Si 401 → tenter un refresh silencieux puis réessayer UNE fois
                if (originalCall.response.status == HttpStatusCode.Unauthorized && token != null) {
                    if (ApiEnvironment.isDebugLoggingEnabled) {
                        println("KTOR: 401 received — attempting token refresh for: ${request.url}")
                    }
                    
                    val newTokens: Pair<String, String>? = refreshMutex.withLock {
                        // Re-check: token may have already been refreshed by another request
                        val currentToken = tokenProvider()
                        if (currentToken != null && currentToken != token) {
                            // Token already refreshed by concurrent call — use new token
                            currentToken to ""
                        } else {
                            // Actually refresh
                            try {
                                refreshTokenProvider(token)
                            } catch (e: Exception) {
                                if (ApiEnvironment.isDebugLoggingEnabled) {
                                    println("KTOR: Token refresh failed: ${e.message}")
                                }
                                null
                            }
                        }
                    }
                    
                    if (newTokens != null) {
                        // Retry with new token
                        val freshToken = tokenProvider() ?: newTokens.first
                        request.headers.remove(HttpHeaders.Authorization)
                        request.header(HttpHeaders.Authorization, "Bearer $freshToken")
                        if (ApiEnvironment.isDebugLoggingEnabled) {
                            println("KTOR: Retrying with refreshed token for: ${request.url}")
                        }
                        execute(request)
                    } else {
                        // Refresh failed — return original 401
                        originalCall
                    }
                } else {
                    originalCall
                }
            }
        }
    }
    
    /**
     * Crée un client HTTP sans authentification.
     * Utilisé pour les endpoints publics (login, register).
     */
    fun createPublic(): HttpClient {
        return HttpClient(createPlatformEngine()) {
            // Timeouts
            install(HttpTimeout) {
                requestTimeoutMillis = 30000  // 30 secondes pour toutes les requêtes
                connectTimeoutMillis = 15000  // 15 secondes pour établir la connexion
                socketTimeoutMillis = 30000   // 30 secondes pour lire/écrire les données
            }
            
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        if (ApiEnvironment.isDebugLoggingEnabled) {
                            println("KTOR: $message")
                        }
                    }
                }
                level = if (ApiEnvironment.isProduction) LogLevel.NONE else LogLevel.INFO
            }
            
            defaultRequest {
                url(BASE_URL)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
        }
    }
}
