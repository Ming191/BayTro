package com.example.baytro.utils.cloudFunctions

import android.util.Log
import com.example.baytro.data.dashboard.LandlordDashboardData
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

class DashboardCloudFunctions(
    private val functions: FirebaseFunctions,
    private val gson: Gson
) {
    suspend fun getLandlordDashboardData(): Result<LandlordDashboardData> {
        return try {
            val result = functions.getHttpsCallable("getLandlordDashboard").call().await()
            val dataMap = result.data as? Map<*, *>
            val dashboardDataMap = dataMap?.get("data")
                ?: return Result.failure(Exception("Response 'data' field is missing or invalid."))

            val json = gson.toJson(dashboardDataMap)
            val dashboardData: LandlordDashboardData = gson.fromJson(json, object : TypeToken<LandlordDashboardData>() {}.type)

            Result.success(dashboardData)
        } catch (e: Exception) {
            Log.e("DashboardFunctions", "Error fetching dashboard data", e)
            val errorMessage = if (e is FirebaseFunctionsException) e.message else "An unexpected error occurred"
            Result.failure(Exception(errorMessage))
        }
    }
}