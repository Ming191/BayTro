package com.example.baytro.utils.cloudFunctions

import android.util.Log
import com.example.baytro.data.Building
import com.example.baytro.data.billing.Bill
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.dashboard.LandlordDashboardData
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.room.Room
import com.example.baytro.data.service.Service
import com.example.baytro.data.user.User
import com.example.baytro.utils.Utils.parseFirebaseError
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

data class TenantDashboardData(
    val user: User?,
    val contract: Contract?,
    val room: Room?,
    val building: Building?,
    val lastApprovedReading: MeterReading?,
    val currentBill: Bill?,
    val fixedServices: List<Service>? = null
)

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

    suspend fun getTenantDashboardData(): Result<TenantDashboardData> {
        return try {
            val result = functions.getHttpsCallable("getTenantDashboardData").call().await()

            val dataMap = result.data as? Map<*, *>
            val dashboardDataMap = dataMap?.get("data")
                ?: return Result.failure(Exception("Response 'data' field is missing or invalid."))

            val json = gson.toJson(dashboardDataMap)
            Log.d("DashboardCloudFunctions", "Received JSON: $json")
            val dashboardData: TenantDashboardData = gson.fromJson(json, object : TypeToken<TenantDashboardData>() {}.type)
            Log.d("DashboardCloudFunctions", "Parsed dashboard data - lastApprovedReading: ${dashboardData.lastApprovedReading}")
            Log.d("DashboardCloudFunctions", "CreatedAt timestamp: ${dashboardData.lastApprovedReading?.createdAt}")

            Result.success(dashboardData)
        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
}