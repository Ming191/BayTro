package com.example.baytro.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class MediaRepository (
    private val storage: FirebaseStorage
) {
    /**
     * Uploads a user's profile image to Firebase Storage.
     *
     * @param userId The ID of the user.
     * @param imageUri The URI of the image to upload.
     * @return The download URL of the uploaded image.
     * @throws Exception if the upload fails.
     */
    suspend fun uploadUserProfileImage(userId: String, imageUri: Uri): String {
        return try {
            val fileName = "users/$userId/profile.jpg"
            val storageRef = storage.reference.child(fileName)
            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload profile image: ${e.message}", e)
        }
    }

    suspend fun uploadBuildingImage(userId: String, buildingId: String, imageUri: Uri): String {
        return try {
            val fileName = "users/$userId/buildings/$buildingId/${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child(fileName)
            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload building image: ${e.message}", e)
        }
    }
}