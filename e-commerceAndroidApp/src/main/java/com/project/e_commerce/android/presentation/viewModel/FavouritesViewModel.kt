package com.project.e_commerce.android.presentation.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.domain.model.Post
import com.project.e_commerce.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * État UI pour l'écran des favoris.
 * 
 * @property isLoading Indique si les données sont en cours de chargement
 * @property bookmarkedPosts Liste des posts mis en favoris
 * @property error Message d'erreur à afficher, null si pas d'erreur
 */
data class FavouritesUiState(
    val isLoading: Boolean = false,
    val bookmarkedPosts: List<Post> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel pour la gestion des favoris (bookmarks).
 * 
 * Gère le chargement des posts mis en favoris par l'utilisateur.
 */
class FavouritesViewModel(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavouritesUiState())
    val uiState: StateFlow<FavouritesUiState> = _uiState.asStateFlow()

    init {
        loadBookmarkedPosts()
    }

    /**
     * Charge les posts mis en favoris par l'utilisateur courant.
     */
    fun loadBookmarkedPosts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Get current user's bookmarked posts (userId will be handled by backend auth)
                val posts = postRepository.getBookmarkedPosts(userId = "")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    bookmarkedPosts = posts,
                    error = null
                )
                Log.d("FavouritesVM", "✅ Loaded ${posts.size} bookmarked posts")
            } catch (e: Exception) {
                Log.e("FavouritesVM", "❌ Error loading bookmarked posts: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load favorites"
                )
            }
        }
    }

    /**
     * Retire un post des favoris.
     * 
     * @param postUid UID du post à retirer des favoris
     */
    fun removeBookmark(postUid: String) {
        viewModelScope.launch {
            try {
                postRepository.unbookmarkPost(postUid, userId = "")
                Log.d("FavouritesVM", "✅ Removed bookmark for post $postUid")
                
                // Reload the list
                loadBookmarkedPosts()
            } catch (e: Exception) {
                Log.e("FavouritesVM", "❌ Error removing bookmark: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to remove from favorites"
                )
            }
        }
    }

    /**
     * Rafraîchit la liste des favoris.
     */
    fun refresh() {
        loadBookmarkedPosts()
    }
}
