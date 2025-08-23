package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.android.data.repository.AuthRepository
import com.project.e_commerce.android.domain.usecase.CheckEmailValidation
import com.project.e_commerce.android.domain.usecase.CheckMatchedPasswordUseCase
import com.project.e_commerce.android.domain.usecase.CheckPasswordValidation
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
    private val repo: AuthRepository,
    private val checkEmailValidation: CheckEmailValidation,
    private val checkPasswordValidation: CheckPasswordValidation,
    private val checkMatchedPassword: CheckMatchedPasswordUseCase,
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
        val result = repo.signIn(s.email.trim(), s.password)

        result.onSuccess {
            _state.value = _state.value.copy(loading = false, isLoggedIn = true)
            _effect.trySend(AuthEffect.NavigateToHome)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, generalError = it.message)
            _effect.trySend(AuthEffect.Toast(it.message ?: "Login failed"))
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
        val result = repo.signUp(s.email.trim(), s.password, s.username.ifBlank { null })

        result.onSuccess {
            _state.value = _state.value.copy(loading = false)
            _effect.trySend(AuthEffect.Toast("Account created successfully"))
            _effect.trySend(AuthEffect.NavigateToLogin)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, generalError = it.message)
            _effect.trySend(AuthEffect.Toast(it.message ?: "Registration failed"))
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
        val result = repo.sendPasswordReset(email.trim())

        result.onSuccess {
            _state.value = _state.value.copy(loading = false)
            _effect.trySend(AuthEffect.Toast("تم إرسال رابط إعادة التعيين إلى بريدك"))
            _effect.trySend(AuthEffect.NavigateToLogin)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, generalError = it.message)
            _effect.trySend(AuthEffect.Toast(it.message ?: "فشل إرسال البريد"))
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


