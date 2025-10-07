package com.example.baytro.view

import com.google.firebase.auth.FirebaseUser

sealed class AuthUIState {
    object Idle : AuthUIState()
    object Loading : AuthUIState()
    data class Success(val user : FirebaseUser) : AuthUIState()
    object PasswordResetSuccess : AuthUIState()
    data class Error(val message: String) : AuthUIState()
    data class NeedVerification(val message: String) : AuthUIState()
    data class FirstTimeUser(val user : FirebaseUser) : AuthUIState()
    data class TenantNoContract(val user : FirebaseUser) : AuthUIState()
    data class TenantPendingSession(val user : FirebaseUser) : AuthUIState()
    data class TenantWithContract(val user : FirebaseUser) : AuthUIState()
}