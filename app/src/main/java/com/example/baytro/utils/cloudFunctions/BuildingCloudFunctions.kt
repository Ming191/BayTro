package com.example.baytro.utils.cloudFunctions

import android.util.Log
import com.example.baytro.data.BuildingListResponse
import com.example.baytro.utils.Utils
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

class BuildingCloudFunctions(
    private val functions: FirebaseFunctions,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "BuildingCloudFunctions"
    }

    /**
     * Calls the backend function to soft-delete (archive) a building.
     *
     * @param buildingId The ID of the building to be archived.
     * @return A Result containing a success message or an Exception on failure.
     */
    suspend fun archiveBuilding(buildingId: String): Result<String> {
        return try {
            val data = hashMapOf("buildingId" to buildingId)

            val result = functions.getHttpsCallable("archiveBuilding")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            val message = response?.get("message") as? String ?: "Building archived successfully."

            Log.d(TAG, "Successfully archived building: $buildingId")
            Result.success(message)

        } catch (e: FirebaseFunctionsException) {
            Log.e(TAG, "Error archiving building $buildingId", e)
            val errorMessage = Utils.parseFirebaseError(e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error archiving building $buildingId", e)
            Result.failure(e)
        }
    }

    suspend fun getBuildingListWithStats(
        searchQuery: String,
        statusFilter: String
    ): Result<BuildingListResponse> {
        return try {
            val data = hashMapOf(
                "searchQuery" to searchQuery,
                "statusFilter" to statusFilter
            )

            Log.d(TAG, "Calling 'getBuildingListWithStats' with data: $data")

            val result = functions.getHttpsCallable("getBuildingListWithStats")
                .call(data)
                .await()

            val dataMap = result.data as? Map<*, *>
            if (dataMap == null) {
                return Result.failure(Exception("Response data is null or not a Map."))
            }

            val json = gson.toJson(dataMap)
            val response: BuildingListResponse = gson.fromJson(json, object : TypeToken<BuildingListResponse>() {}.type)

            Log.d(TAG, "Successfully fetched ${response.buildings.size} buildings.")
            Result.success(response)

        } catch (e: FirebaseFunctionsException) {
            val errorMessage = Utils.parseFirebaseError(e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching building list", e)
            Result.failure(e)
        }
    }
}