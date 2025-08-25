package com.project.e_commerce.android.presentation.viewModel.editProfileViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.usecase.GetUserProfileUseCase
import com.project.e_commerce.android.domain.usecase.UpdateUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val profile: UserProfile? = null,
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String = "",
    val phone: String = "",
    val profileImageUrl: String? = null
)

class EditProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        loadCurrentUserProfile()
    }

    fun loadCurrentUserProfile() {
        val currentUser = auth.currentUser
        Log.d("EditProfileViewModel", "🔍 Current user: ${currentUser?.uid ?: "NULL"}")
        
        if (currentUser != null) {
            if (currentUser.uid.isBlank()) {
                Log.e("EditProfileViewModel", "❌ Current user UID is blank")
                _uiState.value = _uiState.value.copy(
                    error = "Invalid user ID"
                )
                return
            }
            
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                Log.d("EditProfileViewModel", "🔄 Loading profile for UID: ${currentUser.uid}")
                getUserProfileUseCase(currentUser.uid).onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = profile,
                        displayName = profile.displayName,
                        username = profile.username,
                        email = profile.email,
                        bio = profile.bio,
                        phone = profile.phone ?: "",
                        profileImageUrl = profile.profileImageUrl
                    )
                    Log.d("EditProfileViewModel", "✅ Profile loaded: ${profile.displayName}")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load profile"
                    )
                    Log.e("EditProfileViewModel", "❌ Failed to load profile: ${error.message}")
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                error = "User not logged in"
            )
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updateBio(bio: String) {
        _uiState.value = _uiState.value.copy(bio = bio)
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone)
    }

    fun updateProfileImage(imageUrl: String?) {
        _uiState.value = _uiState.value.copy(profileImageUrl = imageUrl)
    }

    fun saveProfile() {
        val currentUser = auth.currentUser
        Log.d("EditProfileViewModel", "💾 Save profile - Current user: ${currentUser?.uid ?: "NULL"}")
        
        if (currentUser != null) {
            if (currentUser.uid.isBlank()) {
                Log.e("EditProfileViewModel", "❌ Current user UID is blank during save")
                _uiState.value = _uiState.value.copy(
                    error = "Invalid user ID"
                )
                return
            }
            
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                Log.d("EditProfileViewModel", "🔄 Creating updated profile for UID: ${currentUser.uid}")
                Log.d("EditProfileViewModel", "🔄 Current profile state: ${_uiState.value.profile}")
                Log.d("EditProfileViewModel", "🔄 Display name: '${_uiState.value.displayName}'")
                Log.d("EditProfileViewModel", "🔄 Username: '${_uiState.value.username}'")
                
                // Always ensure the UID is set correctly
                val updatedProfile = if (_uiState.value.profile != null) {
                    _uiState.value.profile!!.copy(
                        uid = currentUser.uid, // Explicitly set UID
                        displayName = _uiState.value.displayName,
                        username = _uiState.value.username,
                        bio = _uiState.value.bio,
                        phone = _uiState.value.phone,
                        profileImageUrl = _uiState.value.profileImageUrl,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    UserProfile(
                        uid = currentUser.uid,
                        email = _uiState.value.email,
                        displayName = _uiState.value.displayName,
                        username = _uiState.value.username,
                        bio = _uiState.value.bio,
                        phone = _uiState.value.phone,
                        profileImageUrl = _uiState.value.profileImageUrl
                    )
                }
                
                Log.d("EditProfileViewModel", "🔄 Updated profile UID: '${updatedProfile.uid}'")
                Log.d("EditProfileViewModel", "🔄 Updated profile displayName: '${updatedProfile.displayName}'")

                Log.d("EditProfileViewModel", "🔄 Calling updateUserProfileUseCase with UID: ${updatedProfile.uid}")
                updateUserProfileUseCase(updatedProfile).onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        profile = profile
                    )
                    Log.d("EditProfileViewModel", "✅ Profile updated successfully")
                    
                    // Refresh the profile data to ensure UI is up to date
                    loadCurrentUserProfile()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update profile"
                    )
                    Log.e("EditProfileViewModel", "❌ Failed to update profile: ${error.message}")
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                error = "User not logged in"
            )
        }
    }

    fun resetSuccessState() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun refreshProfile() {
        loadCurrentUserProfile()
    }
}
