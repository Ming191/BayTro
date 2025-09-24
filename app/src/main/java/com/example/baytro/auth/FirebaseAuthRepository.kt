package com.example.baytro.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override suspend fun signUp(
        email: String,
        password: String
    ): FirebaseUser {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign up failed")
            firebaseUser
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): FirebaseUser {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign in failed")
            firebaseUser
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun sendVerificationEmail() {
        val firebaseUser = auth.currentUser
            ?: throw IllegalStateException("No logged in user found")

        try {
            firebaseUser.sendEmailVerification().await()
        } catch (e: Exception) {
            throw Exception("Failed to send verification email", e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun checkVerification(): Boolean {
        val firebaseUser: FirebaseUser? = auth.currentUser
        return firebaseUser?.isEmailVerified ?: false
    }

    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    override suspend fun deleteCurrentUser() {
        auth.currentUser?.delete()?.await()
    }
}
