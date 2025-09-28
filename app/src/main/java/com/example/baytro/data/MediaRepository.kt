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

    /**
     * Uploads a contract photo to Firebase Storage.
     *
     * @param contractId The ID of the contract.
     * @param photoIndex The index of the photo (e.g., 0, 1, 2).
     * @param imageFile The image file to upload.
     * @return The download URL of the uploaded photo.
     * @throws Exception if the upload fails.
     */
    suspend fun uploadContractPhoto(contractId: String, photoIndex: Int, imageFile: File): String {
        return try {
            val fileName = "contracts/$contractId/photo_$photoIndex.jpg"
            val storageRef = storage.reference.child(fileName)
            val uri = Uri.fromFile(imageFile)
            storageRef.putFile(uri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload contract photo: ${e.message}", e)
        }
    }
}