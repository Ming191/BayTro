package com.example.baytro.viewModel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.ChangePasswordFormState
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import com.google.firebase.auth.EmailAuthProvider
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
        val current = _changePasswordFormState.value

        val passwordValidation = Validator.validatePassword(current.password)
        val newPasswordValidation = Validator.validatePassword(current.newPassword)
        val newPasswordStrengthValidation = Validator.validatePasswordStrength(current.newPassword)
        val confirmPasswordValidation = Validator.validateConfirmPassword(current.password, current.newPassword)
        val confirmNewPasswordValidation = Validator.validateConfirmPassword(current.newPassword, current.confirmNewPassword)

        val updatedState = current.copy(
            passwordError = passwordValidation,
            newPasswordError = newPasswordValidation,
            newPasswordStrengthError = newPasswordStrengthValidation,
            confirmNewPasswordError = confirmNewPasswordValidation
        )
        _changePasswordFormState.value = updatedState

        val hasError = listOf(
            passwordValidation,
            newPasswordValidation,
            newPasswordStrengthValidation,
            confirmNewPasswordValidation
        ).any { it is ValidationResult.Error }

        if (hasError) return

        viewModelScope.launch {
            val user = authRepository.getCurrentUser()

            if (user == null) {
                _changePasswordUIState.value = AuthUIState.Error("User not logged in")
                return@launch
            }

            val userEmail = user.email
            if (userEmail.isNullOrEmpty()) {
                _changePasswordUIState.value = AuthUIState.Error("No email found for user")
                return@launch
            }

            val credential = EmailAuthProvider.getCredential(userEmail, current.password)
            _changePasswordUIState.value = AuthUIState.Loading

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(current.newPassword)
                        .addOnSuccessListener {
                            _changePasswordUIState.value = AuthUIState.PasswordChangedSuccess
                        }
                        .addOnFailureListener { e ->
                            _changePasswordUIState.value =
                                AuthUIState.Error("Failed to update password: ${e.message}")
                        }
                }
                .addOnFailureListener {
                    _changePasswordFormState.value =
                        current.copy(passwordError = ValidationResult.Error("Incorrect password"))
                    _changePasswordUIState.value = AuthUIState.Error("Wrong current password")
                }
        }
    }
}