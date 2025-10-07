package com.example.baytro.viewModel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.ForgotPasswordFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ForgotPasswordVM(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _forgotPasswordFormState = MutableStateFlow(ForgotPasswordFormState())
    val forgotPasswordFormState: StateFlow<ForgotPasswordFormState> = _forgotPasswordFormState

    private val _forgotPasswordUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val forgotPasswordUIState: StateFlow<AuthUIState> = _forgotPasswordUIState

    fun onEmailChange(email: String) {
        _forgotPasswordFormState.value = _forgotPasswordFormState.value.copy(
            email = email,
            emailError = ValidationResult.Success
        )
    }

    fun resetPassword() {
        val currentState = _forgotPasswordFormState.value
        
        // Validate email
        val emailValidation = Validator.validateEmail(currentState.email)
        
        if (emailValidation is ValidationResult.Error) {
            _forgotPasswordFormState.value = currentState.copy(
                emailError = emailValidation
            )
            return
        }

        // Proceed with password reset
        viewModelScope.launch {
            _forgotPasswordUIState.value = AuthUIState.Loading
            try {
                authRepository.sendPasswordResetEmail(currentState.email)
                _forgotPasswordUIState.value = AuthUIState.PasswordResetSuccess
            } catch (e: Exception) {
                _forgotPasswordUIState.value = AuthUIState.Error(
                    e.message ?: "Failed to send password reset email"
                )
            }
        }
    }
}
