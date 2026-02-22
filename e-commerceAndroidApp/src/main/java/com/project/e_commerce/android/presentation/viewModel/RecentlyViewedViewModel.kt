package com.project.e_commerce.android.presentation.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.android.data.repository.RecentlyViewedRepository
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RecentlyViewedViewModel(
    private val recentlyViewedRepository: RecentlyViewedRepository
) : ViewModel() {

    private val _recentlyViewedReels = MutableStateFlow<List<Reels>>(emptyList())
    val recentlyViewedReels: StateFlow<List<Reels>> = _recentlyViewedReels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadRecentlyViewedReels()
    }

    fun loadRecentlyViewedReels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            recentlyViewedRepository.getRecentlyViewedReels()
                .catch { e ->
                    Log.e("RecentlyViewedViewModel", "Error loading recently viewed reels", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { reels ->
                    Log.d("RecentlyViewedViewModel", "Loaded ${reels.size} recently viewed reels")
                    _recentlyViewedReels.value = reels
                    _isLoading.value = false
                }
        }
    }

    fun addReelToRecentlyViewed(reel: Reels) {
        Log.d("RecentlyViewedViewModel", "Adding reel ${reel.id} to recently viewed")

        try {
            recentlyViewedRepository.addReelToRecentlyViewed(reel)
        } catch (e: Exception) {
            Log.e("RecentlyViewedViewModel", "Error calling repository", e)
        }

        // Refresh the list to show the new item
        loadRecentlyViewedReels()
    }

    fun clearRecentlyViewed() {
        Log.d("RecentlyViewedViewModel", "Clearing recently viewed reels")
        viewModelScope.launch {
            recentlyViewedRepository.clearRecentlyViewed()
            _recentlyViewedReels.value = emptyList()
        }
    }

    fun refreshRecentlyViewed() {
        loadRecentlyViewedReels()
    }

}