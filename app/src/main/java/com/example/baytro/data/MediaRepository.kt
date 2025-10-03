package com.example.baytro.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MediaRepository (
    private val storage: FirebaseStorage
) {

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

    /**
     * Uploads a user image to Firebase Storage and returns the download URL.
     *
     * @param userId The ID of the user.
     * @param imageUri The URI of the image to upload.
     * @param subfolder The subfolder within the user's folder to store the image.
     * @param imageName The name to assign to the uploaded image file (without extension).
     * @return The download URL of the uploaded image.
     * @throws Exception if the upload fails.
     */
    suspend fun uploadUserImage(
        userId: String,
        imageUri: Uri,
        subfolder: String,
        imageName: String): String
    {
        return try {
            val fileName = "users/$userId/$subfolder/$imageName.jpg"
            val storageRef = storage.reference.child(fileName)
            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload profile image: ${e.message}", e)
        }
    }


    /**
     * Uploads an ID card photo to Firebase Storage.
     *
     * @param userId The ID of the user.
     * @param photoIndex The index of the photo (0 for front, 1 for back).
     * @param imageFile The image file to upload.
     * @return The download URL of the uploaded photo.
     * @throws Exception if the upload fails.
     */
    suspend fun uploadIdCardPhoto(userId: String, photoIndex: Int, imageFile: File): String {
        return try {
            val fileName = "users/$userId/id_card/photo_$photoIndex.jpg"
            val storageRef = storage.reference.child(fileName)
            val uri = Uri.fromFile(imageFile)
            storageRef.putFile(uri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload ID card photo: ${e.message}", e)
        }
    }

    /**
     * Downloads an image from an URL and saves it to a temporary file in the cache directory.
     *
     * @param firebaseUrl The URL of the image.
     * @param cacheDir The directory to cache the downloaded image.
     * @return The [File] object representing the downloaded image.
     * @throws Exception if the download fails.
     */
    suspend fun getImageFromUrl(firebaseUrl: String, cacheDir: File): File {
        return withContext(Dispatchers.IO) {
            val tempFile = File(cacheDir, "temp.jpg")

            URL(firebaseUrl).openStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        }
    }
}