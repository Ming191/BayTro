package com.example.baytro.utils.cloudFunctions

import androidx.compose.runtime.Stable
import com.example.baytro.data.contract.Contract
import com.example.baytro.utils.Utils.parseFirebaseError
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

@Stable
data class ContractWithRoom(
    val contractId: String,
    val contract: Contract,
    val roomNumber: String
)

@Stable
data class ContractListResponse(
    val contracts: List<ContractWithRoom> = emptyList()
)

@Stable
data class EndContractResponse(
    val status: String,
    val message: String,
    val unpaidBillsCount: Int? = null,
    val totalUnpaidAmount: Double? = null,
    val warnings: List<String>? = null
)

class ContractCloudFunctions(
    private val functions: FirebaseFunctions,
    private val gson: Gson
) {
    suspend fun getContractList(
        statusFilter: String,
        buildingIdFilter: String?,
        searchQuery: String
    ): Result<ContractListResponse> {
        return try {
            val data = hashMapOf(
                "statusFilter" to statusFilter,
                "buildingIdFilter" to buildingIdFilter,
                "searchQuery" to searchQuery
            )

            val result = functions.getHttpsCallable("getContractList")
                .call(data)
                .await()

            val dataMap = result.data as? Map<*, *>
            if (dataMap == null) {
                return Result.failure(Exception("Response data is null or not a Map."))
            }

            val json = gson.toJson(dataMap)
            val response: ContractListResponse = gson.fromJson(json, object : TypeToken<ContractListResponse>() {}.type)

            Result.success(response)

        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endContract(
        contractId: String,
        forceEnd: Boolean = false
    ): Result<EndContractResponse> {
        return try {
            val data = hashMapOf(
                "contractId" to contractId,
                "forceEnd" to forceEnd
            )

            val result = functions.getHttpsCallable("endContract")
                .call(data)
                .await()

            val dataMap = result.data as? Map<*, *>
            if (dataMap == null) {
                return Result.failure(Exception("Response data is null or not a Map."))
            }

            val json = gson.toJson(dataMap)
            val response: EndContractResponse = gson.fromJson(json, object : TypeToken<EndContractResponse>() {}.type)

            Result.success(response)

        } catch (e: FirebaseFunctionsException) {
            val errorMessage = parseFirebaseError(e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}