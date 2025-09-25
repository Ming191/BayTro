package com.example.baytro.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MediaRepository (
    private val storage: FirebaseStorage
) {
    suspend fun uploadUserProfileImage(userId: String, imageUri: Uri): String {
        return try {
            val fileName = "users/$userId/profile_${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(fileName)

            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload profile image: ${e.message}", e)
        }
    }
}