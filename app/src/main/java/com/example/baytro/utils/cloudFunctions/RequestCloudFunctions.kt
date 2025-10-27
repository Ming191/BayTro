package com.example.baytro.utils.cloudFunctions

import android.util.Log
import com.example.baytro.data.request.FullRequestInfo
import com.example.baytro.utils.Utils.parseFirebaseError
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

data class RequestListResponse(
    val requests: List<FullRequestInfo> = emptyList(),
    val nextCursor: String? = null
)

class RequestCloudFunctions(
    private val functions: FirebaseFunctions,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "RequestCloudFunctions"
    }

    suspend fun getRequestList(
        buildingIdFilter: String?,
        limit: Int = 10,
        startAfter: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<RequestListResponse> {
        return try {
            val data = hashMapOf(
                "buildingIdFilter" to buildingIdFilter,
                "limit" to limit,
                "startAfter" to startAfter,
                "fromDate" to fromDate,   // 👈 thêm vào
                "toDate" to toDate
            )

            if (fromDate != null) data["fromDate"] = fromDate
            if (toDate != null) data["toDate"] = toDate

            Log.d(TAG, "Calling 'getRequestList' with data: $data")
            val result = functions.getHttpsCallable("getRequestList")
                .call(data)
                .await()
            val dataMap = result.data as? Map<*, *>
            if (dataMap == null) {
                return Result.failure(Exception("Response data is null or not a Map."))
            }
            val json = gson.toJson(dataMap)
            val response: RequestListResponse = gson.fromJson(json, object : TypeToken<RequestListResponse>() {}.type)
            Result.success(response)

        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}