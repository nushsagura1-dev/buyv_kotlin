package com.project.e_commerce.data.remote

import com.project.e_commerce.getPlatform

/**
 * Centralized API environment configuration.
 *
 * Controls the base URL used for all API calls.
 * Switch between environments by setting [current] before initializing the HTTP client.
 *
 * Usage:
 *   // In Application.onCreate (Android) or App init (iOS):
 *   ApiEnvironment.current = ApiEnvironment.PRODUCTION
 */
object ApiEnvironment {

    /**
     * Available environments.
     */
    enum class Environment(val label: String) {
        /** Local development — auto-detects simulator vs device */
        DEV("Development"),
        /** Staging server for QA testing */
        STAGING("Staging"),
        /** Production server */
        PRODUCTION("Production")
    }

    /**
     * Current active environment.
     * Defaults to DEV. Set to PRODUCTION before release builds.
     */
    var current: Environment = Environment.DEV

    // ══════════════════════════════════════════════
    // Configure these URLs for your deployment
    // ══════════════════════════════════════════════

    /** Production API URL (HTTPS required) */
    private const val PRODUCTION_URL = "https://buyv-api.up.railway.app"

    /** Staging API URL */
    private const val STAGING_URL = "https://buyv-api-staging.up.railway.app"

    /** Local dev — Android emulator (10.0.2.2 maps to host loopback) */
    private const val DEV_ANDROID_EMULATOR_URL = "http://10.0.2.2:8000"

    /** Local dev — Android physical device (update to your local IP) */
    private const val DEV_ANDROID_DEVICE_URL = "http://192.168.11.108:8000"

    /** Local dev — iOS simulator (loopback) */
    private const val DEV_IOS_SIMULATOR_URL = "http://127.0.0.1:8000"

    /**
     * Returns the base URL for the current environment and platform.
     */
    val baseUrl: String
        get() = when (current) {
            Environment.PRODUCTION -> PRODUCTION_URL
            Environment.STAGING -> STAGING_URL
            Environment.DEV -> getDevUrl()
        }

    private fun getDevUrl(): String {
        val platform = getPlatform().name
        return when {
            platform.contains("Android") -> DEV_ANDROID_DEVICE_URL
            else -> DEV_IOS_SIMULATOR_URL
        }
    }

    /**
     * Whether the current environment is a production environment.
     * Use this to disable debug logging, verbose errors, etc.
     */
    val isProduction: Boolean
        get() = current == Environment.PRODUCTION

    /**
     * Whether debug logging should be enabled.
     */
    val isDebugLoggingEnabled: Boolean
        get() = current != Environment.PRODUCTION
}
