package com.example.baytro.view

import com.google.firebase.auth.FirebaseUser

sealed class SignInUiState {
    object Idle : SignInUiState()
    object Loading : SignInUiState()
    data class Success(val user: FirebaseUser) : SignInUiState()
    data class Error(val message: String) : SignInUiState()
    data class NeedVerification(val message: String) : SignInUiState()
    data class FirstTimeUser(val user: FirebaseUser) : SignInUiState()
    data class TenantNoContract(val user: FirebaseUser) : SignInUiState()
    data class TenantPendingSession(val user: FirebaseUser) : SignInUiState()
    data class TenantWithContract(val user: FirebaseUser) : SignInUiState()
}

