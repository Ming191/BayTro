package com.example.baytro.view

import com.google.firebase.auth.FirebaseUser

sealed class AuthUIState {
    object Idle : AuthUIState()
    object Loading : AuthUIState()
    data class Success(val user : FirebaseUser) : AuthUIState()
    data class Error(val message: String) : AuthUIState()
    data class NeedVerification(val message: String) : AuthUIState()
}