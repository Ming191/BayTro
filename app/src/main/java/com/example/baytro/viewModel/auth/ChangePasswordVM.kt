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
        val user = authRepository.getCurrentUser()

        if (user == null) {
            _changePasswordUIState.value = AuthUIState.Error("User not logged in")
            return
        }

        val userEmail = user.email ?: return

        // Xác thực lại (reauthenticate) để kiểm tra password hiện tại
        val credential = EmailAuthProvider.getCredential(userEmail, current.password)

        viewModelScope.launch {
            _changePasswordUIState.value = AuthUIState.Loading
            user.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Mật khẩu đúng — giờ mới validate tiếp các bước đổi mật khẩu
                        val newPasswordValidation = Validator.validatePassword(current.newPassword)
                        val confirmPasswordValidation = Validator.validateConfirmPassword(
                            current.newPassword,
                            current.confirmNewPassword
                        )

                        val hasError = listOf(
                            newPasswordValidation,
                            confirmPasswordValidation
                        ).any { it is ValidationResult.Error }

                        if (hasError) {
                            _changePasswordFormState.value = current.copy(
                                newPasswordError = newPasswordValidation,
                                confirmNewPasswordError = confirmPasswordValidation
                            )
                            _changePasswordUIState.value = AuthUIState.Error("Invalid new password format")
                            return@addOnCompleteListener
                        }

                        // Nếu ok thì tiến hành đổi mật khẩu
                        user.updatePassword(current.newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    _changePasswordUIState.value = AuthUIState.PasswordChangedSuccess
                                } else {
                                    _changePasswordUIState.value =
                                        AuthUIState.Error("Failed to update password")
                                }
                            }

                    } else {
                        // Sai password hiện tại
                        _changePasswordFormState.value = current.copy(
                            passwordError = ValidationResult.Error("Incorrect password")
                        )
                        _changePasswordUIState.value = AuthUIState.Error("Wrong current password")
                    }
                }
        }
    }
}