package com.example.baytro.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.firebase.auth.FirebaseAuth

class AuthStateHolder {
    var currentUser by mutableStateOf(FirebaseAuth.getInstance().currentUser)
        private set

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            currentUser = auth.currentUser
        }
    }
}

val LocalAuthState = staticCompositionLocalOf { AuthStateHolder() }