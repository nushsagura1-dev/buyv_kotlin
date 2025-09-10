package com.project.e_commerce.android.presentation.viewModel.restPasswordViewModel

import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.project.e_commerce.android.domain.usecase.CheckMatchedPasswordUseCase
import com.project.e_commerce.android.domain.usecase.CheckPasswordValidation
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.changePasswordScreen.ChangePasswordUIState
import com.project.e_commerce.android.presentation.viewModel.baseViewModel.BaseViewModel
import com.project.e_commerce.android.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class RestPasswordViewModel(
    private val checkPasswordValidation: CheckPasswordValidation,
    private val checkMatchedPasswordUseCase: CheckMatchedPasswordUseCase,
    private val authRepository: AuthRepository
) : RestPasswordInteraction, BaseViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUIState())
    val state : StateFlow<ChangePasswordUIState> get() = _state

    override fun onWriteNewPassword(newPassword: String) {
        val validation = checkPasswordValidation(newPassword)
        val copyState = _state.value.copy(
            newPassword = _state.value.newPassword.copy(
                value = newPassword,
                isError = validation.first,
                errorMessage = validation.second
            )
        )
        viewModelScope.launch {
            _state.emit(copyState)
        }
    }

    override fun onWriteNewPasswordConfirmed(newPassword: String) {
        val validation = checkPasswordValidation(newPassword)
        val copyState = _state.value.copy(
            confirmNewPassword = _state.value.confirmNewPassword.copy(
                value = newPassword,
                isError = validation.first,
                errorMessage = validation.second
            )
        )
        viewModelScope.launch {
            _state.emit(copyState)
        }
    }

    override fun onClickChangePassword(navController: NavController) {
        setLoadingState(true)
        val newPassword = _state.value.newPassword.value
        val confirmNewPassword = _state.value.confirmNewPassword.value

        viewModelScope.launch {
            if (checkMatchedPasswordUseCase.invoke(newPassword, confirmNewPassword)) {
                try {
                    // Use Firebase to actually change the password
                    val result = authRepository.changePassword(newPassword)
                    result.fold(
                        onSuccess = {
                            // Sign out the user after password change to force fresh login
                            authRepository.signOut()

                            setLoadingState(false)
                            setErrorState(false, "")
                            val copyState = _state.value.copy(
                                isSuccessChanged = true
                            )
                            _state.emit(copyState)
                            // Navigate to login screen after a short delay
                            delay(1000)
                            navController.navigate(Screens.LoginScreen.route) {
                                // Clear back stack to prevent going back to settings
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onFailure = { exception ->
                            setLoadingState(false)
                            setErrorState(
                                true,
                                exception.message ?: "حدث خطأ أثناء تغيير كلمة المرور"
                            )
                        }
                    )
                } catch (e: Exception) {
                    setLoadingState(false)
                    setErrorState(true, "حدث خطأ غير متوقع: ${e.message}")
                }
            } else {
                setLoadingState(false)
                setErrorState(true, "كلمة المرور غير مطابقة")
            }
        }
    }

    override fun onClickBackArrowButton(navController: NavController) {
        navController.popBackStack()
    }

    override fun setLoadingState(loadingState: Boolean) {
        val copyState = _state.value.copy(
            isLoading = loadingState
        )
        viewModelScope.launch {
            _state.emit(copyState)
        }
    }

    override fun setErrorState(errorState: Boolean, errorMessage: String) {
        val copyState = _state.value.copy(
            isError = errorState,
            errorMessage = errorMessage
        )
        viewModelScope.launch {
            _state.emit(copyState)
        }
    }
}