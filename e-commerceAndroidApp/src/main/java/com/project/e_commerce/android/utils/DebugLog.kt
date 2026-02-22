package com.project.e_commerce.android.utils

import android.util.Log
import com.project.e_commerce.android.BuildConfig

/**
 * Utilitaire de logging qui ne produit des logs qu'en mode DEBUG.
 * 
 * En mode RELEASE, tous les appels sont supprimés par ProGuard/R8,
 * ce qui améliore la sécurité et les performances.
 * 
 * Usage:
 * ```kotlin
 * DebugLog.d("MyTag", "Debug message")
 * DebugLog.i("MyTag", "Info message")
 * DebugLog.w("MyTag", "Warning message")
 * DebugLog.e("MyTag", "Error message", exception)
 * ```
 */
object DebugLog {
    
    private const val DEFAULT_TAG = "BuyV"
    
    /**
     * Log niveau DEBUG - utilisé pour les messages de développement.
     * Supprimé automatiquement en release par ProGuard.
     */
    @JvmStatic
    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Log niveau INFO - utilisé pour les événements informatifs.
     * Supprimé automatiquement en release par ProGuard.
     */
    @JvmStatic
    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
    
    /**
     * Log niveau VERBOSE - utilisé pour les détails très verbeux.
     * Supprimé automatiquement en release par ProGuard.
     */
    @JvmStatic
    fun v(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }
    
    /**
     * Log niveau WARNING - conservé en release pour les avertissements importants.
     */
    @JvmStatic
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * Log niveau ERROR - conservé en release pour les erreurs critiques.
     */
    @JvmStatic
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Vérifie si les logs debug sont activés.
     */
    @JvmStatic
    fun isDebugEnabled(): Boolean = BuildConfig.DEBUG
}
