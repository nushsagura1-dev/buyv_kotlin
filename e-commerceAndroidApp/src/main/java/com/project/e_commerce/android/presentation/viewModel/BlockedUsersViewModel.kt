package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.domain.model.BlockedUser
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.usecase.blockeduser.GetBlockedUsersUseCase
import com.project.e_commerce.domain.usecase.blockeduser.UnblockUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUsersUiState(
    val blockedUsers: List<BlockedUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val unblockingUserId: String? = null,
    val snackbarMessage: String? = null
)

class BlockedUsersViewModel(
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val unblockUserUseCase: UnblockUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    fun loadBlockedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getBlockedUsersUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, blockedUsers = result.data)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.error.message)
                    }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun unblockUser(userUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(unblockingUserId = userUid) }
            when (val result = unblockUserUseCase(userUid)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            unblockingUserId = null,
                            blockedUsers = state.blockedUsers.filter { it.blockedUid != userUid },
                            snackbarMessage = "User unblocked"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            unblockingUserId = null,
                            snackbarMessage = "Failed to unblock: ${result.error.message}"
                        )
                    }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "BlockedUsersVM"
    }
}
