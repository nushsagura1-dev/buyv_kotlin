package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.usecase.sound.GetSoundDetailsUseCase
import com.project.e_commerce.domain.usecase.sound.IncrementSoundUsageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for SoundPageScreen
 * Loads sound details from the shared KMP layer via use cases
 */
class SoundPageViewModel(
    private val getSoundDetailsUseCase: GetSoundDetailsUseCase,
    private val incrementSoundUsageUseCase: IncrementSoundUsageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoundPageUiState())
    val uiState: StateFlow<SoundPageUiState> = _uiState.asStateFlow()

    /**
     * Load sound page data from the backend via shared use case.
     * @param soundUid The unique identifier of the sound to load
     */
    fun loadSoundData(soundUid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = getSoundDetailsUseCase(soundUid)) {
                is Result.Success -> {
                    val sound = result.data
                    _uiState.value = SoundPageUiState(
                        isLoading = false,
                        soundTitle = sound.title,
                        soundAuthor = sound.artist,
                        postsCount = "${sound.usageCount} posts",
                        audioUrl = sound.audioUrl,
                        coverImageUrl = sound.coverImageUrl ?: "",
                        genre = sound.genre ?: "",
                        duration = sound.duration.toInt(),
                        isFavorite = false,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
                is Result.Loading -> {
                    // Already handled above
                }
            }
        }
    }

    /**
     * Increment usage count when a user selects this sound
     */
    fun useSound(soundUid: String) {
        viewModelScope.launch {
            incrementSoundUsageUseCase(soundUid)
        }
    }

    fun toggleFavorite() {
        _uiState.value = _uiState.value.copy(
            isFavorite = !_uiState.value.isFavorite
        )
    }
}

data class SoundPageUiState(
    val isLoading: Boolean = false,
    val soundTitle: String = "",
    val soundAuthor: String = "",
    val postsCount: String = "",
    val audioUrl: String = "",
    val coverImageUrl: String = "",
    val genre: String = "",
    val duration: Int = 0,
    val isFavorite: Boolean = false,
    val error: String? = null
)
