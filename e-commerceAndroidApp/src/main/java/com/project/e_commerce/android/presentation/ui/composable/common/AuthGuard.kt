package com.project.e_commerce.android.presentation.ui.composable.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.navigation.NavController
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.data.local.CurrentUserProvider
import org.koin.compose.koinInject

/**
 * AUTH-003 / AUTH-004: Auth guard composable.
 *
 * Renders [content] when the user is authenticated.
 * When [isAuthenticated] is false it navigates to [Screens.LoginScreen.route] immediately
 * so that every gated action (Like, Comment, Cart, Purchase…) redirects guests to login
 * without crashing or skipping the check.
 *
 * Usage:
 * ```
 * AuthGuard(navController = navController) {
 *     // Only rendered for authenticated users
 *     CartScreen(navController)
 * }
 * ```
 */
@Composable
fun AuthGuard(
    navController: NavController,
    currentUserProvider: CurrentUserProvider = koinInject(),
    content: @Composable () -> Unit
) {
    // isAuthenticated() is a suspend function — collect via produceState
    val isAuthenticated by produceState<Boolean?>(initialValue = null) {
        value = currentUserProvider.isAuthenticated()
    }
    when (isAuthenticated) {
        true -> content()
        false -> {
            // Redirect to login, clearing back stack up to ReelsScreen so back press
            // after login returns to the feed instead of a blank screen.
            LaunchedEffect(Unit) {
                navController.navigate(Screens.LoginScreen.route) {
                    popUpTo(Screens.ReelsScreen.route) { inclusive = false }
                }
            }
        }
        null -> { /* still resolving auth state — render nothing */ }
    }
}

