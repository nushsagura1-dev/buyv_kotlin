package com.project.e_commerce.android.presentation.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.usecase.user.DeleteAccountUseCase
import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.data.local.CurrentUserProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * State for account deletion
 */
data class DeleteAccountUiState(
    val isDeleting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for account deletion
 */
class DeleteAccountViewModel(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val tokenManager: TokenManager,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = DeleteAccountUiState(isDeleting = true)

            when (val result = deleteAccountUseCase()) {
                is Result.Success -> {
                    _uiState.value = DeleteAccountUiState(isSuccess = true)
                    // Backend logout: clear tokens and cache
                    tokenManager.clearTokens()
                    currentUserProvider.clearCache()
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.value = DeleteAccountUiState(
                        error = result.error.message
                    )
                }
                is Result.Loading -> { /* Already showing loading */ }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Confirmation dialog for account deletion.
 * Shows warning message and requires user confirmation.
 *
 * Usage:
 * ```
 * var showDeleteDialog by remember { mutableStateOf(false) }
 *
 * if (showDeleteDialog) {
 *     DeleteAccountDialog(
 *         onConfirm = { showDeleteDialog = false },
 *         onDismiss = { showDeleteDialog = false }
 *     )
 * }
 * ```
 */
@Composable
fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: DeleteAccountViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmationText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!uiState.isDeleting) onDismiss() },
        title = {
            Text(
                text = "Delete Account?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.error
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "⚠️ This action cannot be undone!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.error
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "All your data will be permanently deleted:",
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "• Posts, reels, and photos",
                    "• Comments and likes",
                    "• Followers and following",
                    "• Orders and commissions",
                    "• Notifications and messages"
                ).forEach { item ->
                    Text(
                        text = item,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Type DELETE to confirm:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type DELETE") },
                    enabled = !uiState.isDeleting,
                    singleLine = true
                )

                // Show error if any
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colors.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colors.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Show loading
                if (uiState.isDeleting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Deleting account...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (confirmationText.trim().equals("DELETE", ignoreCase = true)) {
                        viewModel.deleteAccount(onSuccess = onConfirm)
                    }
                },
                enabled = confirmationText.trim().equals("DELETE", ignoreCase = true) && !uiState.isDeleting
            ) {
                Text(
                    "Delete Forever",
                    color = if (confirmationText.trim().equals("DELETE", ignoreCase = true))
                        MaterialTheme.colors.error
                    else
                        Color.Gray
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isDeleting
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Button to trigger account deletion.
 * Opens DeleteAccountDialog on click.
 */
@Composable
fun DeleteAccountButton(
    modifier: Modifier = Modifier,
    onAccountDeleted: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colors.error
        )
    ) {
        Text(
            text = "Delete Account",
            fontWeight = FontWeight.Medium
        )
    }

    if (showDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDialog = false
                onAccountDeleted()
            },
            onDismiss = {
                showDialog = false
            }
        )
    }
}
