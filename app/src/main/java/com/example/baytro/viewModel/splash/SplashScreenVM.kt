package com.example.baytro.viewModel.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.RoleType
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashScreenVM(
    val authRepository: AuthRepository,
) : ViewModel() {


    private val _splashUiState = MutableStateFlow<UiState<RoleType>>(UiState.Idle)
    val splashUiState: StateFlow<UiState<RoleType>> = _splashUiState

    private val _splashFormState = MutableStateFlow(SplashFormState())
    val splashFormState: StateFlow<SplashFormState> = _splashFormState

    fun onComplete() {
        viewModelScope.launch {
            _splashUiState.value = UiState.Loading

            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _splashUiState.value = UiState.Error("User not logged in")
                return@launch
            }
            val role = splashFormState.value.role
            _splashUiState.value = UiState.Success(role)
        }
    }

    fun onRoleChange(role: RoleType) {
        _splashFormState.value = _splashFormState.value.copy(role = role)
    }

    fun clearError() {
        _splashUiState.value = UiState.Idle
    }
}