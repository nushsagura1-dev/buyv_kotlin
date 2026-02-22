package com.project.e_commerce.android.presentation.utils

import android.content.Context
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Singleton pour gérer le cache vidéo ExoPlayer.
 * 
 * Avantages :
 * - Réduit l'utilisation de la bande passante
 * - Améliore la performance de lecture (pas de rebuffering)
 * - Expérience utilisateur fluide (lecture instantanée des vidéos déjà vues)
 * 
 * Configuration :
 * - Cache max : 500 MB (configurable)
 * - Stratégie : LRU (Least Recently Used) - supprime les vidéos les moins récemment lues
 * - Stockage : dossier cache de l'app (nettoyé automatiquement par Android si besoin)
 */
object VideoPlayerCache {
    
    private const val TAG = "VideoPlayerCache"
    private const val CACHE_DIR_NAME = "exoplayer_cache"
    private const val MAX_CACHE_SIZE_BYTES = 500L * 1024 * 1024 // 500 MB
    
    @Volatile
    private var simpleCache: SimpleCache? = null
    
    @Volatile
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null
    
    /**
     * Initialise le cache ExoPlayer.
     * Doit être appelé une seule fois au démarrage de l'app (dans Application.onCreate()).
     * 
     * @param context Context de l'application
     */
    @Synchronized
    fun initialize(context: Context) {
        if (simpleCache != null) {
            Log.d(TAG, "Cache already initialized, skipping")
            return
        }
        
        try {
            Log.d(TAG, "Initializing ExoPlayer cache with max size: ${MAX_CACHE_SIZE_BYTES / (1024 * 1024)} MB")
            
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
                Log.d(TAG, "Created cache directory: ${cacheDir.absolutePath}")
            }
            
            // Créer le cache avec stratégie LRU
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
            val databaseProvider = StandaloneDatabaseProvider(context)
            
            simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)
            Log.d(TAG, "✅ ExoPlayer cache initialized successfully")
            
            // Créer la factory pour les DataSource avec cache
            val upstreamFactory = DefaultDataSource.Factory(
                context,
                DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(15000)  // 15s connexion timeout
                    .setReadTimeoutMs(30000)      // 30s lecture timeout
                    .setUserAgent("BuyV-Android/1.0")
            )
            
            cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCache!!)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(null) // Utilise le cache par défaut
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR) // Ignore le cache en cas d'erreur
            
            Log.d(TAG, "✅ CacheDataSource.Factory configured")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize ExoPlayer cache", e)
            simpleCache = null
            cacheDataSourceFactory = null
        }
    }
    
    /**
     * Retourne la factory pour créer des DataSource avec cache.
     * Utilisé lors de la création de MediaSource dans ExoPlayer.
     * 
     * @param context Context pour créer un fallback si le cache n'est pas initialisé
     * @return CacheDataSource.Factory configurée ou DefaultDataSource.Factory en fallback
     */
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        // Si le cache n'est pas initialisé, l'initialiser maintenant
        if (cacheDataSourceFactory == null) {
            Log.w(TAG, "Cache not initialized, initializing now...")
            initialize(context)
        }
        
        // Retourner la factory avec cache ou un fallback sans cache
        return cacheDataSourceFactory ?: run {
            Log.w(TAG, "⚠️ Cache unavailable, using DefaultDataSource.Factory (no cache)")
            DefaultDataSource.Factory(
                context,
                DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(15000)
                    .setReadTimeoutMs(30000)
                    .setUserAgent("BuyV-Android/1.0")
            )
        }
    }
    
    /**
     * Retourne le cache ExoPlayer.
     * Utilisé pour créer des CacheDataSource personnalisées si besoin.
     */
    fun getCache(): Cache? = simpleCache
    
    /**
     * Libère les ressources du cache.
     * Doit être appelé lors de la fermeture de l'app (optionnel).
     */
    @Synchronized
    fun release() {
        try {
            Log.d(TAG, "Releasing ExoPlayer cache")
            simpleCache?.release()
            simpleCache = null
            cacheDataSourceFactory = null
            Log.d(TAG, "✅ Cache released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error releasing cache", e)
        }
    }
    
    /**
     * Vide le cache (supprime toutes les vidéos en cache).
     * Utile pour libérer de l'espace ou pour le debugging.
     */
    @Synchronized
    fun clearCache() {
        try {
            Log.d(TAG, "Clearing ExoPlayer cache")
            simpleCache?.let { cache ->
                val keys = cache.keys
                keys.forEach { key ->
                    try {
                        cache.removeResource(key)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing cache key: $key", e)
                    }
                }
                Log.d(TAG, "✅ Cache cleared successfully (${keys.size} items removed)")
            } ?: Log.w(TAG, "Cache not initialized, nothing to clear")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing cache", e)
        }
    }
    
    /**
     * Retourne la taille actuelle du cache en bytes.
     */
    fun getCacheSizeBytes(): Long {
        return try {
            simpleCache?.cacheSpace ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache size", e)
            0L
        }
    }
    
    /**
     * Retourne la taille actuelle du cache en MB (formatée).
     */
    fun getCacheSizeMB(): String {
        val bytes = getCacheSizeBytes()
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.2f MB", mb)
    }
    
    /**
     * Vérifie si le cache est initialisé et fonctionnel.
     */
    fun isCacheAvailable(): Boolean {
        return simpleCache != null && cacheDataSourceFactory != null
    }
}
