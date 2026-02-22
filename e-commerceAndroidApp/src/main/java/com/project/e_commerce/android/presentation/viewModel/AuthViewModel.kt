package com.project.e_commerce.android.presentation.viewModel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.android.data.repository.AuthRepository
import com.project.e_commerce.android.domain.usecase.CheckEmailValidation
import com.project.e_commerce.android.domain.usecase.CheckMatchedPasswordUseCase
import com.project.e_commerce.android.domain.usecase.CheckPasswordValidation
import com.project.e_commerce.android.domain.usecase.GoogleSignInUseCase
import com.project.e_commerce.android.data.helper.GoogleSignInHelper
import com.project.e_commerce.domain.usecase.auth.LoginUseCase
import com.project.e_commerce.domain.usecase.auth.RegisterUseCase
import com.project.e_commerce.domain.usecase.auth.LogoutUseCase
import com.project.e_commerce.domain.usecase.auth.SendPasswordResetUseCase
import com.project.e_commerce.domain.model.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val rePassword: String = "",
    val loading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val rePasswordError: String? = null,
    val generalError: String? = null,
    val isLoggedIn: Boolean = false,
)

sealed class AuthEffect {
    data object NavigateToHome : AuthEffect()
    data object NavigateToLogin : AuthEffect()
    data class Toast(val message: String) : AuthEffect()
}

class AuthViewModel(
    private val repo: AuthRepository? = null, // TODO: Remove after full backend migration
    private val checkEmailValidation: CheckEmailValidation,
    private val checkPasswordValidation: CheckPasswordValidation,
    private val checkMatchedPassword: CheckMatchedPasswordUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase? = null, // TODO: Implement in backend
    private val googleSignInHelper: GoogleSignInHelper,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // -------- inputs ----------
    fun onEmailChanged(v: String) {
        _state.value = _state.value.copy(email = v, emailError = null, generalError = null)
    }

    fun onPasswordChanged(v: String) {
        _state.value = _state.value.copy(password = v, passwordError = null, generalError = null)
    }

    fun onRePasswordChanged(v: String) {
        _state.value = _state.value.copy(rePassword = v, rePasswordError = null, generalError = null)
    }

    fun onUsernameChanged(v: String) {
        _state.value = _state.value.copy(username = v, generalError = null)
    }

    // -------- actions ----------
    fun login() = viewModelScope.launch {
        val s = _state.value

        val emailErr = pairToError(checkEmailValidation(s.email))
        val passErr  = pairToError(checkPasswordValidation(s.password))

        if (emailErr != null || passErr != null) {
            _state.value = s.copy(emailError = emailErr, passwordError = passErr)
            return@launch
        }

        _state.value = s.copy(loading = true, generalError = null)
        
        // Use KMP LoginUseCase
        when (val result = loginUseCase(s.email.trim(), s.password)) {
            is Result.Success -> {
                _state.value = _state.value.copy(loading = false, isLoggedIn = true)
                _effect.trySend(AuthEffect.NavigateToHome)
            }
            is Result.Error -> {
                _state.value = _state.value.copy(loading = false, generalError = result.error.message)
                _effect.trySend(AuthEffect.Toast(result.error.message ?: "Login failed"))
            }
            is Result.Loading -> {
                // Loading is handled by local state
            }
        }
    }

    fun register() = viewModelScope.launch {
        val s = _state.value

        val emailErr = pairToError(checkEmailValidation(s.email))
        val passErr  = pairToError(checkPasswordValidation(s.password))

        val matchRes: Any? = checkMatchedPassword(s.password, s.rePassword)
        val matchErr = matchRes.matchErrorOrNull()


        if (emailErr != null || passErr != null || matchErr != null) {
            _state.value = s.copy(
                emailError = emailErr,
                passwordError = passErr,
                rePasswordError = matchErr
            )
            return@launch
        }

        _state.value = s.copy(loading = true, generalError = null)
        
        // Use KMP RegisterUseCase
        when (val result = registerUseCase(s.email.trim(), s.password, s.username.ifBlank { "User" })) {
            is Result.Success -> {
                _state.value = _state.value.copy(loading = false)
                _effect.trySend(AuthEffect.Toast("Account created successfully"))
                _effect.trySend(AuthEffect.NavigateToLogin)
            }
            is Result.Error -> {
                _state.value = _state.value.copy(loading = false, generalError = result.error.message)
                _effect.trySend(AuthEffect.Toast(result.error.message ?: "Registration failed"))
            }
            is Result.Loading -> {}
        }
    }

    fun sendResetEmail(emailInput: String? = null) = viewModelScope.launch {
        val email = emailInput ?: _state.value.email
        val emailErr = pairToError(checkEmailValidation(email))
        if (emailErr != null) {
            _state.value = _state.value.copy(email = email, emailError = emailErr)
            return@launch
        }

        _state.value = _state.value.copy(loading = true, generalError = null)
        // Use KMP SendPasswordResetUseCase
        when (val result = sendPasswordResetUseCase(email.trim())) {
            is Result.Success -> {
                _state.value = _state.value.copy(loading = false)
                _effect.trySend(AuthEffect.Toast("تم إرسال رابط إعادة التعيين إلى بريدك"))
                _effect.trySend(AuthEffect.NavigateToLogin)
            }
            is Result.Error -> {
                _state.value = _state.value.copy(loading = false, generalError = result.error.message)
                _effect.trySend(AuthEffect.Toast(result.error.message ?: "فشل إرسال البريد"))
            }
            is Result.Loading -> {}
        }
    }

    fun getGoogleSignInIntent(): Intent? {
        return if (googleSignInHelper.isGooglePlayServicesAvailable()) {
            googleSignInHelper.getSignInIntent()
        } else {
            _effect.trySend(AuthEffect.Toast("Google Play Services not available"))
            null
        }
    }

    fun handleGoogleSignInResult(data: Intent?) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, generalError = null)

        if (googleSignInUseCase == null) {
            _state.value = _state.value.copy(loading = false, generalError = "Google Sign-In not available")
            _effect.trySend(AuthEffect.Toast("Google Sign-In temporarily unavailable"))
            return@launch
        }

        val tokenResult = googleSignInHelper.handleSignInResult(data)
        tokenResult.fold(
            onSuccess = { idToken ->
                when (val result = googleSignInUseCase(idToken)) {
                    is Result.Success -> {
                        _state.value = _state.value.copy(loading = false, isLoggedIn = true)
                        _effect.trySend(AuthEffect.NavigateToHome)
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(loading = false, generalError = result.error.message)
                        _effect.trySend(AuthEffect.Toast(result.error.message ?: "Google sign in failed"))
                    }
                    is Result.Loading -> {}
                }
            },
            onFailure = { exception ->
                _state.value = _state.value.copy(loading = false, generalError = exception.message)
                _effect.trySend(AuthEffect.Toast(exception.message ?: "Google sign in failed"))
            }
        )
    }

    fun signOut() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)

        // Sign out from Google
        googleSignInHelper.signOut()

        // Use KMP LogoutUseCase
        when (val result = logoutUseCase()) {
            is Result.Success -> {
                _state.value = AuthUiState() // Reset to initial state
                _effect.trySend(AuthEffect.NavigateToLogin)
            }
            is Result.Error -> {
                _state.value = _state.value.copy(loading = false, generalError = result.error.message)
                _effect.trySend(AuthEffect.Toast("Sign out failed: ${result.error.message}"))
            }
            is Result.Loading -> {}
        }
    }
}

/* ===================== Helpers ===================== */

private fun pairToError(
    p: Pair<Boolean, *>,
    defaultMsg: String? = null
): String? {
    return if (p.first) null
    else (p.second as? String)?.takeIf { it.isNotBlank() } ?: defaultMsg
}

private fun Any?.matchErrorOrNull(defaultMsg: String = "Passwords do not match"): String? {
    return when (this) {
        is Pair<*, *> -> {
            val ok  = this.first as? Boolean ?: false
            val msg = this.second as? String
            if (ok) null else msg?.takeIf { it.isNotBlank() } ?: defaultMsg
        }
        is Boolean -> if (this) null else defaultMsg
        is String  -> this
        null       -> null
        else       -> null
    }
}
