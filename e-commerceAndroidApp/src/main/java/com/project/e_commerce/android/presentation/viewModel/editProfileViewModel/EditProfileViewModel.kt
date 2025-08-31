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

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo
import com.project.e_commerce.android.data.remote.CloudinaryConfig
import com.project.e_commerce.android.presentation.utils.ProfileImageCache
import com.project.e_commerce.android.presentation.utils.UserInfoCache

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

    fun updateProfileImage(imageUri: String?) {
        Log.d("EditProfileViewModel", "🖼️ ===== PROFILE IMAGE UPDATE =====")
        Log.d("EditProfileViewModel", "🖼️ New image URI: '$imageUri'")
        Log.d(
            "EditProfileViewModel",
            "🖼️ Is local URI: ${imageUri?.startsWith("content://") == true}"
        )
        Log.d("EditProfileViewModel", "🖼️ Is http URL: ${imageUri?.startsWith("http") == true}")

        if (imageUri?.startsWith("content://") == true) {
            Log.d("EditProfileViewModel", "🖼️ Starting Cloudinary upload for local URI")
            uploadProfileImageToCloudinary(imageUri)
        } else {
            // It's already a URL, just update the state
            _uiState.value = _uiState.value.copy(profileImageUrl = imageUri)
            Log.d("EditProfileViewModel", "🖼️ Using existing URL without upload")
        }

        Log.d("EditProfileViewModel", "🖼️ ===================================")
    }

    private fun uploadProfileImageToCloudinary(localUri: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("EditProfileViewModel", "❌ No authenticated user for image upload")
            _uiState.value = _uiState.value.copy(error = "User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("EditProfileViewModel", "🚀 Starting profile image upload to Cloudinary")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Set up Cloudinary upload options for profile images
                val imageOptions = com.cloudinary.utils.ObjectUtils.asMap(
                    "public_id",
                    "profile_${currentUser.uid}_${System.currentTimeMillis()}",
                    "upload_preset",
                    com.project.e_commerce.android.data.remote.CloudinaryConfig.UPLOAD_PRESET,
                    "folder",
                    com.project.e_commerce.android.data.remote.CloudinaryConfig.Folders.USERS,
                    "transformation",
                    "w_400,h_400,c_fill,g_face,q_auto"
                )

                Log.d("EditProfileViewModel", " Upload options: $imageOptions")

                // Upload using MediaManager
                com.cloudinary.android.MediaManager.get().upload(android.net.Uri.parse(localUri))
                    .options(imageOptions)
                    .callback(object : com.cloudinary.android.callback.UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d("EditProfileViewModel", "🚀 Upload started: $requestId")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                            val cloudinaryUrl = resultData["secure_url"] as? String
                            Log.d("EditProfileViewModel", "✅ Upload successful!")
                            Log.d("EditProfileViewModel", "🖼️ Cloudinary URL: $cloudinaryUrl")

                            // Clear profile image cache so new image shows up immediately
                            currentUser.uid?.let { userId ->
                                ProfileImageCache.clearUserCache(userId)
                                UserInfoCache.clearUserCache(userId)
                                Log.d(
                                    "EditProfileViewModel",
                                    "🧹 Cleared profile image and user info cache for user: $userId"
                                )
                            }

                            // Update UI state with new URL
                            _uiState.value = _uiState.value.copy(
                                profileImageUrl = cloudinaryUrl,
                                isLoading = false,
                                error = null
                            )

                            Log.d("EditProfileViewModel", "✅ Profile image URL updated in UI state")
                        }

                        override fun onError(
                            requestId: String,
                            error: com.cloudinary.android.callback.ErrorInfo
                        ) {
                            Log.e("EditProfileViewModel", "❌ Upload failed: ${error.description}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to upload image: ${error.description}"
                            )
                        }

                        override fun onReschedule(
                            requestId: String,
                            error: com.cloudinary.android.callback.ErrorInfo
                        ) {
                            Log.w(
                                "EditProfileViewModel",
                                "⚠️ Upload rescheduled: ${error.description}"
                            )
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes * 100 / totalBytes).toInt()
                            Log.d("EditProfileViewModel", "📊 Upload progress: $progress%")
                        }
                    }).dispatch()

            } catch (e: Exception) {
                Log.e("EditProfileViewModel", "❌ Exception during upload: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Upload failed: ${e.message}"
                )
            }
        }
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

                Log.d("EditProfileViewModel", "🖼️ ===== SAVE PROFILE IMAGE DEBUG =====")
                Log.d(
                    "EditProfileViewModel",
                    "🖼️ Current profileImageUrl in UI state: '${_uiState.value.profileImageUrl}'"
                )
                Log.d(
                    "EditProfileViewModel",
                    "🖼️ Original profile profileImageUrl: '${_uiState.value.profile?.profileImageUrl}'"
                )
                Log.d(
                    "EditProfileViewModel",
                    "🖼️ Will save profileImageUrl: '${_uiState.value.profileImageUrl}'"
                )
                Log.d("EditProfileViewModel", "🖼️ ========================================")

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
                    // Clear profile caches after successful profile update
                    ProfileImageCache.clearUserCache(updatedProfile.uid)
                    UserInfoCache.clearUserCache(updatedProfile.uid)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        profile = profile,
                        // Update the current state to match the saved profile
                        profileImageUrl = profile.profileImageUrl,
                        displayName = profile.displayName,
                        username = profile.username,
                        bio = profile.bio,
                        phone = profile.phone ?: ""
                    )
                    Log.d("EditProfileViewModel", " Profile updated successfully")
                    Log.d(
                        "EditProfileViewModel",
                        " Updated profile image URL: '${profile.profileImageUrl}'"
                    )

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
