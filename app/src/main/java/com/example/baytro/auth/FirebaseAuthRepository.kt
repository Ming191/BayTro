package com.example.baytro.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository (
    private val auth: FirebaseAuth
) : AuthRepository {
    override suspend fun signUp(email: String, password: String): FirebaseUser {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.sendEmailVerification()?.await()
            return result.user!!
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun signIn(email: String, password: String): FirebaseUser {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            return authResult.user!!
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun sendVerificationEmail(user: FirebaseUser) {
        try {
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            throw e
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}