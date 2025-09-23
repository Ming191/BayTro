package com.example.baytro.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.SignUpFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignUpVM (
    private val authRepository: AuthRepository,
    private val validator: Validator
) : ViewModel() {
    private val _signUpUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val signUpUIState: StateFlow<AuthUIState> = _signUpUIState

    private val _signUpFormState = MutableStateFlow(SignUpFormState())
    val signUpFormState: StateFlow<SignUpFormState> = _signUpFormState

    fun onEmailChange(email: String) {
        _signUpFormState.value =
            _signUpFormState.value.copy(
                email = email,
                emailError = ValidationResult.Success)
    }

    fun onPasswordChange(password: String) {
        _signUpFormState.value = _signUpFormState.value.copy(
            password = password,
            passwordError = ValidationResult.Success,
            passwordStrengthError = ValidationResult.Success
        )
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _signUpFormState.value = _signUpFormState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = ValidationResult.Success,
            passwordMatchError = ValidationResult.Success
        )
    }

    private fun validateInput(
        formState: SignUpFormState
    ): Boolean {
        val emailValidator = validator.validateEmail(formState.email)
        val passwordValidator = validator.validatePassword(formState.password)
        val passwordStrengthValidator = validator.validatePasswordStrength(formState.password)
        val confirmPasswordValidator = validator.validateConfirmPassword(formState.password, formState.confirmPassword)
        val isValid = emailValidator == ValidationResult.Success
                && passwordValidator == ValidationResult.Success
                && passwordStrengthValidator == ValidationResult.Success
                && confirmPasswordValidator == ValidationResult.Success

        _signUpFormState.value = formState.copy(
            emailError = emailValidator,
            passwordError = passwordValidator,
            passwordStrengthError = passwordStrengthValidator,
            confirmPasswordError = confirmPasswordValidator,
        )
        return isValid
    }

    fun signUp() {
        val formState = _signUpFormState.value
        if (validateInput(formState)) {
            performSignUp(formState.email, formState.password)
        }
    }

    private fun performSignUp(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _signUpUIState.value = AuthUIState.Loading
            try {
                authRepository.signUp(email, password)
                authRepository.signOut()
                _signUpUIState.value = AuthUIState.NeedVerification("Please check your email for verification")
            } catch (e: Exception) {
                _signUpUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}