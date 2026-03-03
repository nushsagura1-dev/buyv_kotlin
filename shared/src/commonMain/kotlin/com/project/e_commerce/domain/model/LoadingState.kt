package com.project.e_commerce.domain.model

/**
 * Centralized loading state used by all ViewModels via MainUiStateViewModel (UI-002).
 *
 * Replaces the multiple independent spinners visible simultaneously.
 * A single [GlobalLoadingOverlay] composable observes this from MainUiStateViewModel.
 */
sealed class LoadingState {

    /** No ongoing operation — default state. */
    data object Idle : LoadingState()

    /**
     * Operation in progress.
     * @param message Optional label shown beneath the spinner (debug/QA only).
     */
    data class Loading(val message: String = "") : LoadingState()

    /**
     * Operation completed with an error.
     * @param message User-visible error description (no stack traces!).
     */
    data class Error(val message: String) : LoadingState()

    /** Operation completed successfully. Transition back to [Idle] after consuming. */
    data object Success : LoadingState()

    val isLoading: Boolean get() = this is Loading
    val isError: Boolean get() = this is Error
    val isIdle: Boolean get() = this is Idle || this is Success
}
