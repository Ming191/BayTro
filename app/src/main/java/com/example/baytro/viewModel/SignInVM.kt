package com.example.baytro.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.SignInFormState
import com.example.baytro.data.UserRepository
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import com.google.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignInVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _signInUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val signInUIState: StateFlow<AuthUIState> = _signInUIState

    private val _signInFormState = MutableStateFlow(SignInFormState())
    val signInFormState: StateFlow<SignInFormState> = _signInFormState

    fun onEmailChange(email: String) {
        _signInFormState.value = _signInFormState.value.copy(email = email, emailError = ValidationResult.Success)
    }

    fun onPasswordChange(password: String) {
        _signInFormState.value = _signInFormState.value.copy(password = password, passwordError = ValidationResult.Success)
    }

    fun login() {
        val formState = _signInFormState.value
        if (validateInput(formState)) {
            performLogin(formState.email, formState.password)
        }
    }

    private fun validateInput(
        formState: SignInFormState
    ): Boolean {
        val emailValidator = Validator.validateEmail(formState.email)
        val passwordValidator = Validator.validatePassword(formState.password)
        val isValid = emailValidator == ValidationResult.Success && passwordValidator == ValidationResult.Success

        _signInFormState.value = formState.copy(
            emailError = emailValidator,
            passwordError = passwordValidator
        )
        return isValid
    }

    private fun performLogin(email: String, password: String) {
        viewModelScope.launch {
            _signInUIState.value = AuthUIState.Loading
            try {
                val user = authRepository.signIn(email, password)

                if (authRepository.checkVerification()) {
                    val repoUser = userRepository.getById(user.uid)
                    if (repoUser == null) {
                        _signInUIState.value = AuthUIState.FirstTimeUser(user)
                    } else {
                        _signInUIState.value = AuthUIState.Success(user)
                    }
                } else {
                    authRepository.sendVerificationEmail()
                    authRepository.signOut()
                    _signInUIState.value = AuthUIState.NeedVerification("Email is not verified. We have sent you a new verification email.")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthException -> when (e.errorCode) {
                        "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Incorrect email or password."
                        "ERROR_USER_DISABLED" -> "Your account has been disabled."
                        else -> "Sign in failed. Please try again."
                    }
                    is java.net.UnknownHostException -> "No network connection."
                    else -> e.message
                }
                _signInUIState.value = AuthUIState.Error(errorMessage ?: "An unknown error occurred")
            }
        }
    }
}