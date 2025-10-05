package com.example.baytro.auth

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
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
            try {
                val fcmToken = Firebase.messaging.token.await()
                updateFcmToken(fcmToken)
                Log.d("Auth", "FCM token updated on login.")
            } catch (e: Exception) {
                Log.e("Auth", "Failed to update FCM token on login", e)
            }
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

    override suspend fun updateFcmToken(token: String) {
        val data = hashMapOf("fcmToken" to token)
        functions
            .getHttpsCallable("updateFcmToken")
            .call(data)
            .await()
    }
}
