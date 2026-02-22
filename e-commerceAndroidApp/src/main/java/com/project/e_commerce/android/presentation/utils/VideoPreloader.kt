package com.project.e_commerce.android.presentation.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestionnaire de preloading pour les vidéos ExoPlayer.
 * 
 * Améliore l'expérience utilisateur en préchargeant les vidéos suivantes pendant que
 * l'utilisateur regarde la vidéo actuelle.
 * 
 * Stratégie :
 * - Précharge automatiquement les 2 vidéos suivantes
 * - Utilise le cache ExoPlayer (VideoPlayerCache)
 * - Nettoie automatiquement les vidéos trop loin de la page actuelle
 * - Thread-safe avec ConcurrentHashMap
 */
object VideoPreloader {
    
    private const val TAG = "VideoPreloader"
    private const val PRELOAD_COUNT = 2 // Nombre de vidéos à précharger en avance
    private const val PRELOAD_DELAY_MS = 500L // Délai avant de commencer le preloading
    
    // Map pour stocker les MediaSource préchargées
    private val preloadedSources = ConcurrentHashMap<String, MediaSource>()
    
    // Map pour tracker les jobs de preloading en cours
    private val preloadJobs = ConcurrentHashMap<String, Job>()
    
    // Scope pour les coroutines de preloading
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Précharge les vidéos suivantes à partir de la page actuelle.
     * 
     * @param context Context pour accéder au cache
     * @param currentIndex Index de la vidéo actuellement affichée
     * @param videoUris Liste de toutes les URIs vidéo
     */
    fun preloadNextVideos(
        context: Context,
        currentIndex: Int,
        videoUris: List<Uri?>
    ) {
        Log.d(TAG, "🔄 Preloading request for index $currentIndex (total: ${videoUris.size})")
        
        // Annuler les preloads précédents qui sont trop loin
        cleanupDistantPreloads(currentIndex, videoUris.size)
        
        // Précharger les N prochaines vidéos
        for (i in 1..PRELOAD_COUNT) {
            val nextIndex = currentIndex + i
            
            if (nextIndex >= videoUris.size) {
                Log.d(TAG, "⏭️ No more videos to preload after index $currentIndex")
                break
            }
            
            val uri = videoUris[nextIndex]
            if (uri == null || uri.toString().isBlank()) {
                Log.w(TAG, "⚠️ Skipping invalid URI at index $nextIndex")
                continue
            }
            
            val key = uri.toString()
            
            // Si déjà préchargé, skip
            if (preloadedSources.containsKey(key)) {
                Log.d(TAG, "✅ Video $nextIndex already preloaded: $key")
                continue
            }
            
            // Si preload déjà en cours, skip
            if (preloadJobs.containsKey(key) && preloadJobs[key]?.isActive == true) {
                Log.d(TAG, "⏳ Video $nextIndex already preloading: $key")
                continue
            }
            
            // Lancer le preload
            Log.d(TAG, "🚀 Starting preload for video $nextIndex: $key")
            val job = preloadScope.launch {
                try {
                    // Petit délai pour ne pas surcharger immédiatement
                    delay(PRELOAD_DELAY_MS * i)
                    
                    preloadVideo(context, uri, key)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error preloading video $nextIndex: ${e.message}", e)
                    preloadJobs.remove(key)
                }
            }
            
            preloadJobs[key] = job
        }
    }
    
    /**
     * Précharge une vidéo spécifique.
     */
    private fun preloadVideo(context: Context, uri: Uri, key: String) {
        try {
            Log.d(TAG, "📥 Preloading video: $key")
            
            // Obtenir la factory de DataSource avec cache
            val cacheDataSourceFactory: DataSource.Factory = VideoPlayerCache.getCacheDataSourceFactory(context)
            
            // Créer le MediaSource
            val mediaItem = MediaItem.fromUri(uri)
            val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(mediaItem)
            
            // Stocker le MediaSource préchargé
            preloadedSources[key] = mediaSource
            
            Log.d(TAG, "✅ Video preloaded successfully: $key (cache: ${VideoPlayerCache.getCacheSizeMB()})")
            
            // Nettoyer le job
            preloadJobs.remove(key)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to preload video: $key", e)
            preloadJobs.remove(key)
        }
    }
    
    /**
     * Récupère un MediaSource préchargé si disponible.
     * 
     * @param uri URI de la vidéo
     * @return MediaSource préchargé ou null si non disponible
     */
    fun getPreloadedSource(uri: Uri?): MediaSource? {
        if (uri == null) return null
        
        val key = uri.toString()
        val source = preloadedSources[key]
        
        if (source != null) {
            Log.d(TAG, "🎯 Using preloaded source for: $key")
        } else {
            Log.d(TAG, "⚠️ No preloaded source available for: $key")
        }
        
        return source
    }
    
    /**
     * Nettoie les MediaSource préchargées qui sont trop loin de la page actuelle.
     * Libère de la mémoire en supprimant les sources dont on n'a plus besoin.
     */
    private fun cleanupDistantPreloads(currentIndex: Int, totalSize: Int) {
        val keysToRemove = mutableListOf<String>()
        
        // Identifier les sources à supprimer (trop loin de currentIndex)
        preloadedSources.keys.forEach { key ->
            // On ne peut pas facilement retrouver l'index depuis la clé,
            // donc on garde une stratégie simple : limite la taille du cache
            if (preloadedSources.size > PRELOAD_COUNT * 2) {
                keysToRemove.add(key)
            }
        }
        
        // Supprimer les anciennes sources
        keysToRemove.forEach { key ->
            preloadedSources.remove(key)
            Log.d(TAG, "🗑️ Cleaned up distant preload: $key")
        }
        
        // Annuler les jobs de preload trop loin
        preloadJobs.keys.forEach { key ->
            val job = preloadJobs[key]
            // Logique simple : si on a trop de jobs en cours, annuler les anciens
            if (preloadJobs.size > PRELOAD_COUNT * 2 && job?.isActive == true) {
                job.cancel()
                preloadJobs.remove(key)
                Log.d(TAG, "🚫 Cancelled distant preload job: $key")
            }
        }
    }
    
    /**
     * Nettoie toutes les sources préchargées et annule tous les jobs.
     * Utile lors de la destruction de l'écran ou changement majeur de contenu.
     */
    fun clearAll() {
        Log.d(TAG, "🧹 Clearing all preloaded sources and jobs")
        
        // Annuler tous les jobs en cours
        preloadJobs.values.forEach { job ->
            job.cancel()
        }
        preloadJobs.clear()
        
        // Supprimer toutes les sources
        preloadedSources.clear()
        
        Log.d(TAG, "✅ All preloads cleared")
    }
    
    /**
     * Retourne le nombre de vidéos actuellement préchargées.
     */
    fun getPreloadedCount(): Int {
        return preloadedSources.size
    }
    
    /**
     * Retourne le nombre de jobs de preload en cours.
     */
    fun getActivePreloadJobsCount(): Int {
        return preloadJobs.count { it.value.isActive }
    }
    
    /**
     * Vérifie si une vidéo est déjà préchargée.
     */
    fun isPreloaded(uri: Uri?): Boolean {
        if (uri == null) return false
        return preloadedSources.containsKey(uri.toString())
    }
}
