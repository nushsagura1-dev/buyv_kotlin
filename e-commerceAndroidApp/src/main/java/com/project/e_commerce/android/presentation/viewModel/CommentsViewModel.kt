package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.domain.model.Comment
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.usecase.comment.AddCommentUseCase
import com.project.e_commerce.domain.usecase.comment.DeleteCommentUseCase
import com.project.e_commerce.domain.usecase.comment.GetCommentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentsUiState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isAddingComment: Boolean = false,
    val error: String? = null,
    val hasMoreComments: Boolean = true,
    val currentPage: Int = 0,
    val commentAddedSuccess: Boolean = false
)

class CommentsViewModel(
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 20
    }

    fun loadComments(postId: String, refresh: Boolean = false) {
        if (refresh) {
            _uiState.update {
                it.copy(
                    comments = emptyList(),
                    currentPage = 0,
                    hasMoreComments = true,
                    error = null
                )
            }
        }

        val currentState = _uiState.value
        if (!currentState.hasMoreComments && !refresh) return
        if (currentState.isLoading || currentState.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = refresh || it.currentPage == 0,
                    isLoadingMore = !refresh && it.currentPage > 0,
                    error = null
                )
            }

            val offset = if (refresh) 0 else currentState.currentPage * PAGE_SIZE
            when (val result = getCommentsUseCase(postId, PAGE_SIZE, offset)) {
                is Result.Success -> {
                    val newComments = result.data
                    val updatedComments = if (refresh) {
                        newComments
                    } else {
                        currentState.comments + newComments
                    }

                    _uiState.update {
                        it.copy(
                            comments = updatedComments,
                            isLoading = false,
                            isLoadingMore = false,
                            hasMoreComments = newComments.size == PAGE_SIZE,
                            currentPage = if (refresh) 1 else it.currentPage + 1
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = result.error.message
                        )
                    }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun addComment(postId: String, content: String, userId: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingComment = true, error = null, commentAddedSuccess = false) }

            when (val result = addCommentUseCase(postId, content, userId)) {
                is Result.Success -> {
                    // Add new comment at the beginning of the list
                    val updatedComments = listOf(result.data) + _uiState.value.comments
                    _uiState.update {
                        it.copy(
                            comments = updatedComments,
                            isAddingComment = false,
                            commentAddedSuccess = true
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isAddingComment = false,
                            error = result.error.message,
                            commentAddedSuccess = false
                        )
                    }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun deleteComment(postId: String, commentId: Int, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            when (val result = deleteCommentUseCase(postId, commentId, userId)) {
                is Result.Success -> {
                    // Remove comment from list
                    val updatedComments = _uiState.value.comments.filter { it.id != commentId }
                    _uiState.update {
                        it.copy(comments = updatedComments)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(error = result.error.message)
                    }
                }
                is Result.Loading -> { /* No loading indicator for delete */ }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearCommentAddedSuccess() {
        _uiState.update { it.copy(commentAddedSuccess = false) }
    }

    fun resetState() {
        _uiState.value = CommentsUiState()
    }
}
