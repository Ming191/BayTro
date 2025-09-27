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