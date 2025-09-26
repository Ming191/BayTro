package com.example.baytro.viewModel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.SignUpFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignUpVM (
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _signUpUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val signUpUIState: StateFlow<AuthUIState> = _signUpUIState

    private val _signUpFormState = MutableStateFlow(SignUpFormState())
    val signUpFormState: StateFlow<SignUpFormState> = _signUpFormState

    fun onEmailChange(email: String) {
        _signUpFormState.value = _signUpFormState.value.copy(
            email = email,
            emailError = ValidationResult.Success
        )
    }

    fun onPasswordChange(password: String) {
        _signUpFormState.value = _signUpFormState.value.copy(
            password = password,
            passwordError = ValidationResult.Success,
            passwordStrengthError = ValidationResult.Success
        )
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        val currentState = _signUpFormState.value
        _signUpFormState.value = currentState.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = ValidationResult.Success,
        )
    }

    private fun validateInput(
        formState: SignUpFormState
    ): Boolean {
        val emailValidator = Validator.validateEmail(formState.email)
        val passwordValidator = Validator.validatePassword(formState.password)
        val passwordStrengthValidator = Validator.validatePasswordStrength(formState.password)
        val confirmPasswordValidator = Validator.validateConfirmPassword(formState.password, formState.confirmPassword)
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
            performSignUp()
        }
    }

    private fun performSignUp() {
        viewModelScope.launch {
            _signUpUIState.value = AuthUIState.Loading
            var currentUser : FirebaseUser? = null
            try {
                val formState = _signUpFormState.value
                authRepository.signUp(formState.email, formState.password)
                currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("Failed to get current user after sign up")
                authRepository.sendVerificationEmail()
                authRepository.signOut()
                _signUpUIState.value = AuthUIState.NeedVerification("Please check your email for verification")
            } catch (e: Exception) {
                if (currentUser != null) {
                    authRepository.deleteCurrentUser()
                }
                _signUpUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}