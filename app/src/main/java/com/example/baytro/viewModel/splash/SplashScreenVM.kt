package com.example.baytro.viewModel.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.FirebaseAuthRepository
import com.example.baytro.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashScreenVM(
    val authRepository: AuthRepository,
    val userRepository: UserRepository
) : ViewModel() {
    private val _splashUiState = MutableStateFlow<SplashUiState>(SplashUiState.Idle)
    val splashUiState : StateFlow<SplashUiState> = _splashUiState

    fun onComplete() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _splashUiState.value = SplashUiState.Error("User not logged in")
                return@launch
            }
            val user = userRepository.getById(currentUser.uid)
            if (user != null) {
                if (user.roleType.name == "LANDLORD") {
                    _splashUiState.value = SplashUiState.LandlordLogin
                } else {
                    _splashUiState.value = SplashUiState.TenantLogin
                }
            } else {
                _splashUiState.value = SplashUiState.Error("User not found")
            }
        }
    }
}