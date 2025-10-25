package com.example.baytro.view

sealed class SignUpUiState {
    object Idle : SignUpUiState()
    object Loading : SignUpUiState()
    data class Error(val message: String) : SignUpUiState()
    data class NeedVerification(val message: String) : SignUpUiState()
}

