package com.example.baytro.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.AuthRepository
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SignUpVM (
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _signUpUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val signUpUIState: MutableStateFlow<AuthUIState> = _signUpUIState

    fun signUp(email: String, password: String, confirmPassword: String) {
        if (
            email.isBlank() || password.isBlank() || confirmPassword.isBlank()
        ) {
            _signUpUIState.value = AuthUIState.Error("Please fill in all fields")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _signUpUIState.value = AuthUIState.Error("Please enter a valid email")
            return
        }
        if (password != confirmPassword) {
            _signUpUIState.value = AuthUIState.Error("Passwords do not match")
            return
        }
        if (password.length < 6) {
            _signUpUIState.value = AuthUIState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _signUpUIState.value = AuthUIState.Loading
            try {
                authRepository.signUp(email, password)
                authRepository.logout()
                _signUpUIState.value = AuthUIState.NeedVerification("Please check your email for verification")
            } catch (e: Exception) {
                _signUpUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}