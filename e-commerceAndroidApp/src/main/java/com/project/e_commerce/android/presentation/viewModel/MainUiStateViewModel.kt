package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainUiStateViewModel : ViewModel() {
    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible

    private val _isAddToCartSheetVisible = MutableStateFlow(false)
    val isAddToCartSheetVisible: StateFlow<Boolean> = _isAddToCartSheetVisible

    private val _selectedReelForCart = MutableStateFlow<Reels?>(null)
    val selectedReelForCart: StateFlow<Reels?> = _selectedReelForCart

    private val _currentReel = MutableStateFlow<Reels?>(null)
    val currentReel: StateFlow<Reels?> = _currentReel

    private val _hideBottomBar = MutableStateFlow(false)
    val hideBottomBar: StateFlow<Boolean> = _hideBottomBar

    fun setCurrentReel(reel: Reels?) {
        _currentReel.value = reel
    }

    fun showAddToCartSheet(reel: Reels) { 
        _selectedReelForCart.value = reel
        _isAddToCartSheetVisible.value = true 
    }
    
    fun hideAddToCartSheet() { 
        _isAddToCartSheetVisible.value = false 
        _selectedReelForCart.value = null
    }

    fun setBottomSheetVisible(visible: Boolean) {
        _isBottomSheetVisible.value = visible
    }

    fun hideBottomBar() {
        _hideBottomBar.value = true
    }

    fun showBottomBar() {
        _hideBottomBar.value = false
    }
}