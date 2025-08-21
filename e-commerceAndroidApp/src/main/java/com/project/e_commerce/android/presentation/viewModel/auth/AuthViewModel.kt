package com.project.e_commerce.android.presentation.viewModel.auth



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.android.domain.usecase.*
import com.project.e_commerce.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginUseCase(email, password).onSuccess {
                _user.value = it
            }.onFailure {
                // Handle error
            }
        }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            registerUseCase(email, password).onSuccess {
                _user.value = it
                onResult(true, null)
            }.onFailure {
                onResult(false, it.message)
            }
        }
    }


    fun forgotPassword(email: String) {
        viewModelScope.launch {
            forgotPasswordUseCase(email)
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _user.value = null
        }
    }
}
