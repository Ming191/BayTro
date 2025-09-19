package com.example.baytro.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.AuthRepository
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInVM(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _signInState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val signInState: MutableStateFlow<AuthUIState> = _signInState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _signInState.value = AuthUIState.Error("Please fill in all fields")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _signInState.value = AuthUIState.Error("Please enter a valid email")
            return
        }
        if (password.length < 6) {
            _signInState.value = AuthUIState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _signInState.value = AuthUIState.Loading
            try {
                val user = authRepository.login(email, password)
                if (user.isEmailVerified) {
                    _signInState.value = AuthUIState.Success(user)
                } else {
                    user.sendEmailVerification().await()
                    authRepository.logout()
                    _signInState.value = AuthUIState.NeedVerification("Please verify your email before logging in.")
                }
            } catch (e: Exception) {
                _signInState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}