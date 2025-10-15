package com.example.baytro.viewModel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.ChangePasswordFormState
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChangePasswordVM (
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _changePasswordFormState = MutableStateFlow(ChangePasswordFormState())
    val changePasswordFormState: StateFlow<ChangePasswordFormState> = _changePasswordFormState
    private val _changePasswordUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val changePasswordUIState: StateFlow<AuthUIState> = _changePasswordUIState

    fun onPasswordChange(password: String) {
        _changePasswordFormState.value = _changePasswordFormState.value.copy(
            password = password,
            passwordError = ValidationResult.Success
        )
    }

    fun onNewPasswordChange(newPassword: String) {
        _changePasswordFormState.value = _changePasswordFormState.value.copy(
            newPassword = newPassword,
            newPasswordError = ValidationResult.Success
        )
    }

    fun onConfirmNewPasswordChange(confirmNewPassword: String) {
        _changePasswordFormState.value = _changePasswordFormState.value.copy(
            confirmNewPassword = confirmNewPassword,
            confirmNewPasswordError = ValidationResult.Success
        )
    }

    fun changePassword() {
        val currentState = _changePasswordFormState.value

        // Validate password
        val passwordValidation = Validator.validatePassword(currentState.password)
        val newPasswordValidator = Validator.validatePassword(currentState.newPassword)
        val newPasswordStrengthValidator = Validator.validatePasswordStrength(currentState.newPassword)
        val confirmPasswordValidator = Validator.validateConfirmPassword(currentState.password, currentState.newPassword)
        val confirmNewPasswordValidator = Validator.validateConfirmPassword(currentState.newPassword, currentState.confirmNewPassword)

        if (passwordValidation is ValidationResult.Error) {
            _changePasswordFormState.value = currentState.copy(
                passwordError = passwordValidation
            )
            return
        }

        if (newPasswordValidator == ValidationResult.Success && newPasswordStrengthValidator == ValidationResult.Success
            && confirmNewPasswordValidator == ValidationResult.Success && confirmPasswordValidator != ValidationResult.Success) {
            // Proceed with change password
            viewModelScope.launch {
                _changePasswordUIState.value = AuthUIState.Loading
                try {
                    val user = authRepository.getCurrentUser()
                    user!!.updatePassword(currentState.newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                _changePasswordUIState.value = AuthUIState.PasswordChangedSuccess
                            }
                        }
                    _changePasswordUIState.value = AuthUIState.Success(user)
                } catch (e: Exception) {
                    _changePasswordUIState.value = AuthUIState.Error(
                        e.message ?: "Failed to change password"
                    )
                }
            }
        } else {
            return
        }
    }
}