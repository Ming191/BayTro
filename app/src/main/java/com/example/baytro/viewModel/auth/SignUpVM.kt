package com.example.baytro.viewModel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.SignUpFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.SignUpUiState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignUpVM (
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _signUpUIState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val signUpUIState: StateFlow<SignUpUiState> = _signUpUIState

    private val _signUpFormState = MutableStateFlow(SignUpFormState())
    val signUpFormState: StateFlow<SignUpFormState> = _signUpFormState

    fun resetState() {
        Log.d("SignUpVM", "resetState() called - Resetting UI state to Idle")
        _signUpUIState.value = SignUpUiState.Idle
        _signUpFormState.value = SignUpFormState()
        Log.d("SignUpVM", "resetState() completed")
    }

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
        Log.d("SignUpVM", "signUp() called")
        val formState = _signUpFormState.value
        Log.d("SignUpVM", "Validating form - Email: ${formState.email}")
        if (validateInput(formState)) {
            Log.d("SignUpVM", "Validation passed - Starting signup process")
            performSignUp()
        } else {
            Log.w("SignUpVM", "Validation failed - Not proceeding with signup")
        }
    }

    private fun performSignUp() {
        Log.d("SignUpVM", "performSignUp() started")
        viewModelScope.launch {
            Log.d("SignUpVM", "Setting state to Loading")
            _signUpUIState.value = SignUpUiState.Loading
            var currentUser : FirebaseUser? = null
            try {
                val formState = _signUpFormState.value
                Log.d("SignUpVM", "Calling authRepository.signUp() for email: ${formState.email}")
                authRepository.signUp(formState.email, formState.password)

                Log.d("SignUpVM", "Getting current user")
                currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("Failed to get current user after sign up")
                Log.d("SignUpVM", "Current user obtained: ${currentUser.uid}")

                Log.d("SignUpVM", "Sending verification email")
                authRepository.sendVerificationEmail()

                Log.d("SignUpVM", "Signing out user")
                authRepository.signOut()

                Log.d("SignUpVM", "Setting state to NeedVerification")
                _signUpUIState.value = SignUpUiState.NeedVerification("Please check your email for verification")
                Log.d("SignUpVM", "SignUp process completed successfully")
            } catch (e: Exception) {
                Log.e("SignUpVM", "SignUp failed with exception: ${e.message}", e)
                if (currentUser != null) {
                    Log.d("SignUpVM", "Deleting user due to error")
                    authRepository.deleteCurrentUser()
                }
                Log.d("SignUpVM", "Setting state to Error")
                _signUpUIState.value = SignUpUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}