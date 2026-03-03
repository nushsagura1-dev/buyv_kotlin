package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.domain.platform.CameraController
import com.project.e_commerce.domain.platform.CaptureState
import com.project.e_commerce.domain.platform.FilterInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 2.18 — CameraViewModel
 *
 * Owns the [CameraController] instance (injected by Koin) and exposes
 * immutable [StateFlow]s consumed by [CameraScreen].
 *
 * Koin module registration  (add to the appModule / cameraModule):
 * ```kotlin
 * viewModel { CameraViewModel(get()) }
 * single { CameraController(androidContext()) }
 * ```
 */
class CameraViewModel(
    val cameraController: CameraController
) : ViewModel() {

    // ──────────────────────────────────
    // State
    // ──────────────────────────────────

    private val _captureState = MutableStateFlow<CaptureState?>(null)
    val captureState: StateFlow<CaptureState?> = _captureState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _selectedFilterId = MutableStateFlow("none")
    val selectedFilterId: StateFlow<String> = _selectedFilterId.asStateFlow()

    val availableFilters: List<FilterInfo> = cameraController.getAvailableFilters()

    private val _lastOutputUri = MutableStateFlow<String?>(null)
    val lastOutputUri: StateFlow<String?> = _lastOutputUri.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ──────────────────────────────────
    // Actions
    // ──────────────────────────────────

    /** Toggle recording on/off. Call from the record button. */
    fun toggleRecording(maxDurationMs: Long = 30_000L) {
        if (_isRecording.value) {
            cameraController.stopRecording()
        } else {
            _isRecording.value = true
            viewModelScope.launch {
                try {
                    cameraController.startRecording(maxDurationMs).collect { state ->
                        _captureState.value = state
                        when (state) {
                            is CaptureState.Recording   -> { /* already flagged above */ }
                            is CaptureState.Completed   -> {
                                _isRecording.value  = false
                                _lastOutputUri.value = state.outputUri
                            }
                            is CaptureState.Error       -> {
                                _isRecording.value   = false
                                _errorMessage.value  = state.message
                            }
                        }
                    }
                } catch (e: Exception) {
                    _isRecording.value  = false
                    _errorMessage.value = e.message
                }
            }
        }
    }

    /** Apply a filter by its [FilterInfo.id]. */
    fun selectFilter(filterId: String) {
        _selectedFilterId.value = filterId
        cameraController.applyFilter(filterId)
    }

    /** Flip between front and back camera. */
    fun flipCamera() = cameraController.flipCamera()

    /** Dismiss the current error snackbar. */
    fun clearError() { _errorMessage.value = null }

    // ──────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        cameraController.release()
    }
}
