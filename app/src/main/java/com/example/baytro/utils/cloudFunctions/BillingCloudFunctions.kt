package com.example.baytro.utils.cloudFunctions

import android.util.Log
import androidx.compose.runtime.Stable
import com.example.baytro.utils.Utils.parseFirebaseError
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

@Stable
data class MarkBillAsPaidResponse(
    val status: String,
    val message: String,
    val billId: String? = null,
    val paidAmount: Double? = null
)

@Stable
data class SendReminderResponse(
    val status: String,
    val message: String,
    val notificationsSent: Int? = null,
    val totalTenants: Int? = null
)

@Stable
data class AddManualChargeResponse(
    val status: String,
    val message: String,
    val billId: String? = null,
    val newTotalAmount: Double? = null
)

class BillingCloudFunctions(
    private val functions: FirebaseFunctions
) {
    companion object {
        private const val TAG = "BillingCloudFunctions"
    }

    /**
     * Mark a bill as paid manually
     * Called by landlord
     */
    suspend fun markBillAsPaid(
        billId: String,
        paidAmount: Double,
        paymentMethod: String
    ): Result<MarkBillAsPaidResponse> {
        return try {
            val data = hashMapOf(
                "billId" to billId,
                "paidAmount" to paidAmount,
                "paymentMethod" to paymentMethod
            )

            val result = functions.getHttpsCallable("markBillAsPaid")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            val markBillResponse = MarkBillAsPaidResponse(
                status = response?.get("status") as? String ?: "success",
                message = response?.get("message") as? String ?: "Bill marked as paid successfully",
                billId = response?.get("billId") as? String,
                paidAmount = (response?.get("paidAmount") as? Number)?.toDouble()
            )

            Log.d(TAG, "Marked bill as paid: $billId")
            Result.success(markBillResponse)
        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Log.e(TAG, "Failed to mark bill as paid: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error marking bill as paid", e)
            Result.failure(e)
        }
    }

    /**
     * Send payment reminder to tenants
     * Called by landlord
     */
    suspend fun sendBillPaymentReminder(
        billId: String,
        customMessage: String = ""
    ): Result<SendReminderResponse> {
        return try {
            val data = hashMapOf(
                "billId" to billId,
                "customMessage" to customMessage
            )

            val result = functions.getHttpsCallable("sendBillPaymentReminder")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            val reminderResponse = SendReminderResponse(
                status = response?.get("status") as? String ?: "success",
                message = response?.get("message") as? String ?: "Reminder sent successfully",
                notificationsSent = (response?.get("notificationsSent") as? Number)?.toInt(),
                totalTenants = (response?.get("totalTenants") as? Number)?.toInt()
            )

            Log.d(TAG, "Sent reminder for bill: $billId, sent: ${reminderResponse.notificationsSent}/${reminderResponse.totalTenants}")
            Result.success(reminderResponse)
        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Log.e(TAG, "Failed to send reminder: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending reminder", e)
            Result.failure(e)
        }
    }

    /**
     * Add manual charge to a bill
     * Called by landlord
     */
    suspend fun addManualChargeToBill(
        billId: String,
        description: String,
        amount: Double
    ): Result<AddManualChargeResponse> {
        return try {
            val data = hashMapOf(
                "billId" to billId,
                "description" to description,
                "amount" to amount
            )

            val result = functions.getHttpsCallable("addManualChargeToBill")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            val chargeResponse = AddManualChargeResponse(
                status = response?.get("status") as? String ?: "success",
                message = response?.get("message") as? String ?: "Manual charge added successfully",
                billId = response?.get("billId") as? String,
                newTotalAmount = (response?.get("newTotalAmount") as? Number)?.toDouble()
            )

            Log.d(TAG, "Added manual charge to bill: $billId, amount: $amount")
            Result.success(chargeResponse)
        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Log.e(TAG, "Failed to add manual charge: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error adding manual charge", e)
            Result.failure(e)
        }
    }
}

