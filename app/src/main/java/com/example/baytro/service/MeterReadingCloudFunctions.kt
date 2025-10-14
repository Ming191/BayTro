package com.example.baytro.service

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

class MeterReadingCloudFunctions(
    private val functions: FirebaseFunctions
) {
    companion object {
        private const val TAG = "MeterReadingCloudFunctions"
    }

    /**
     * Notify landlord about new meter reading submission
     * Called by tenant after submitting a reading
     */
    suspend fun notifyNewMeterReading(readingId: String): Result<Unit> {
        return try {
            val data = hashMapOf("readingId" to readingId)
            functions.getHttpsCallable("notifyNewMeterReading")
                .call(data)
                .await()

            Log.d(TAG, "Notification sent for reading: $readingId")
            Result.success(Unit)
        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Log.e(TAG, "Failed to send notification: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending notification", e)
            Result.failure(e)
        }
    }

    /**
     * Approve a meter reading
     * Called by landlord
     */
    suspend fun approveMeterReading(readingId: String): Result<String> {
        return try {
            val data = hashMapOf("readingId" to readingId)
            val result = functions.getHttpsCallable("approveMeterReading")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            val message = response?.get("message") as? String ?: "Reading approved successfully"

            Log.d(TAG, "Approved reading: $readingId")
            Result.success(message)
        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Log.e(TAG, "Failed to approve reading: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error approving reading", e)
            Result.failure(e)
        }
    }

    /**
     * Decline a meter reading with reason
     * Called by landlord
     */
    suspend fun declineMeterReading(readingId: String, reason: String): Result<String> {
        return try {
            val data = hashMapOf(
                "readingId" to readingId,
                "reason" to reason
            )
            val result = functions.getHttpsCallable("declineMeterReading")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            val message = response?.get("message") as? String ?: "Reading declined successfully"

            Log.d(TAG, "Declined reading: $readingId, reason: $reason")
            Result.success(message)
        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Log.e(TAG, "Failed to decline reading: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error declining reading", e)
            Result.failure(e)
        }
    }

    private fun parseFirebaseError(e: FirebaseFunctionsException): String {
        val details = e.details
        if (details is Map<*, *>) {
            return details["message"] as? String ?: e.message ?: "An error occurred"
        }
        return when (e.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "User not authenticated"
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Permission denied"
            FirebaseFunctionsException.Code.NOT_FOUND -> "Resource not found"
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> "Invalid argument provided"
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> "Operation failed: invalid state"
            else -> e.message ?: "An error occurred"
        }
    }
}

