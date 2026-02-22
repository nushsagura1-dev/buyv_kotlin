package com.project.e_commerce.android.presentation.ui.screens.reelsScreen

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.components.CommentsBottomSheet
import com.project.e_commerce.android.presentation.viewModel.CommentsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Wrapper for displaying CommentsBottomSheet with proper state management.
 * This can be integrated into ReelsView or used standalone.
 *
 * Usage:
 * ```
 * var showCommentsSheet by remember { mutableStateOf(false) }
 * var currentPostId by remember { mutableStateOf<String?>(null) }
 *
 * // In your UI button:
 * onClick = {
 *     currentPostId = postId
 *     showCommentsSheet = true
 * }
 *
 * // At bottom of composable:
 * if (showCommentsSheet && currentPostId != null) {
 *     CommentsSheet(
 *         postId = currentPostId!!,
 *         onDismiss = { showCommentsSheet = false }
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CommentsSheet(
    postId: String,
    onDismiss: () -> Unit,
    commentsViewModel: CommentsViewModel = koinViewModel()
) {
    val commentsUiState by commentsViewModel.uiState.collectAsState()
    val currentUserProvider: com.project.e_commerce.data.local.CurrentUserProvider = org.koin.compose.koinInject()
    var currentUserId by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        currentUserId = currentUserProvider.getCurrentUserId() ?: ""
    }

    // Reset state when sheet closes
    DisposableEffect(Unit) {
        onDispose {
            commentsViewModel.resetState()
        }
    }

    CommentsBottomSheet(
        postId = postId,
        currentUserId = currentUserId,
        commentsUiState = commentsUiState,
        onDismiss = onDismiss,
        onLoadComments = { postIdParam ->
            commentsViewModel.loadComments(postIdParam, refresh = true)
        },
        onAddComment = { postIdParam, content, userId ->
            commentsViewModel.addComment(postIdParam, content, userId)
        },
        onDeleteComment = { postIdParam, commentId, userId ->
            commentsViewModel.deleteComment(postIdParam, commentId, userId)
        },
        onClearError = {
            commentsViewModel.clearError()
        }
    )
}
