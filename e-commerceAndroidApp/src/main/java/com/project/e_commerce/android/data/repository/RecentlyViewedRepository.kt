package com.project.e_commerce.android.data.repository

import android.util.Log
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date

/**
 * Stub repository for recently viewed reels.
 * TODO: Migrate to backend API when endpoint is implemented.
 */
class RecentlyViewedRepository(
    private val currentUserProvider: com.project.e_commerce.data.local.CurrentUserProvider
) {
    companion object {
        private const val MAX_RECENTLY_VIEWED = 50
    }

    // In-memory cache for recently viewed (temporary until backend is implemented)
    private val recentlyViewedCache = mutableListOf<Reels>()

    // ⚠️ NOTE: Backend endpoint POST /recently-viewed not yet implemented
    fun addReelToRecentlyViewed(reel: Reels) {
        Log.d("RecentlyViewedRepository", "Adding reel ${reel.id} to cache (backend not yet implemented)")
        // Remove if already exists
        recentlyViewedCache.removeAll { it.id == reel.id }
        // Add to front
        recentlyViewedCache.add(0, reel)
        // Keep max size
        if (recentlyViewedCache.size > MAX_RECENTLY_VIEWED) {
            recentlyViewedCache.removeAt(recentlyViewedCache.lastIndex)
        }
    }

    fun getRecentlyViewedReels(): Flow<List<Reels>> = flow {
        Log.d("RecentlyViewedRepository", "Returning ${recentlyViewedCache.size} cached recently viewed reels")
        emit(recentlyViewedCache.toList())
    }

    fun clearRecentlyViewed() {
        Log.d("RecentlyViewedRepository", "Clearing recently viewed cache")
        recentlyViewedCache.clear()
    }
}