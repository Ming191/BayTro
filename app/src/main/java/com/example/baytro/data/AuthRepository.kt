package com.example.baytro.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository (
    private val auth : FirebaseAuth
) {
    fun getCurrentUser() : FirebaseUser? {
        return auth.currentUser
    }

    suspend fun signUp(email: String, password: String): FirebaseUser {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            return result.user!!
        } catch (e: Exception) {
            throw e
        }
    }
}