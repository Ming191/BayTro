package com.example.baytro.data.contract

import android.util.Log
import com.example.baytro.data.Repository
import com.example.baytro.data.room.Room
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull

class ContractRepository(
    db : FirebaseFirestore
) : Repository<Contract> {
    private val collection = db.collection("contracts")

    override suspend fun getAll(): List<Contract> {
        val snapshot = collection.get()
        return snapshot.documents.map {
            it.data<Contract>().copy(id = it.id)
        }
    }

    override suspend fun getById(id: String): Contract? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val contract = snapshot.data<Contract>()
            contract.copy(id = snapshot.id)
        } else {
            null
        }
    }

    override suspend fun add(item: Contract): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: Contract) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: Contract) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }
    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    suspend fun isUserInAnyContract(userId: String): Boolean {
        return try {
            val snapshot = collection.where {
                all(
                    "tenantIds" contains userId,
                    "status" equalTo "ACTIVE"
                )
            }.get()

            snapshot.documents.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun getContractFlow(contractId: String): Flow<Contract?> {
        if (contractId.isBlank()) {
            return flowOf(null)
        }
        val docRef = collection.document(contractId)
        return docRef.snapshots
            .mapNotNull { documentSnapshot ->
                if (documentSnapshot.exists) {
                    documentSnapshot.data<Contract>()
                } else {
                    null
                }
            }
    }

    suspend fun getContractsByStatus(landlordId: String, statuses: List<Status>): List<Contract> {
        if (landlordId.isBlank() || statuses.isEmpty()) {
            Log.d("ContractRepository", "Query skipped: landlordId is blank or statuses is empty")
            return emptyList()
        }
        Log.d("ContractRepository", "Querying for landlordId: '$landlordId', statuses: ${statuses.map { it.name }}")
        try {
            val querySnapshot = collection.where {
                all(
                    "landlordId" equalTo landlordId,
                    "status" inArray statuses.map { it.name }
                )
            }.get()
            Log.d("ContractRepository", "Found ${querySnapshot.documents.size} documents for landlordId: '$landlordId', statuses: ${statuses.map { it.name }}")
            return querySnapshot.documents.map { doc ->
                val contract = doc.data<Contract>()
                Log.d("ContractRepository", "Fetched contract: ${contract.contractNumber}, landlordId: ${contract.landlordId}, status: ${contract.status}")
                contract.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("ContractRepository", "Error fetching contracts by status", e)
            throw e
        }
    }

    suspend fun getContractsByBuildingId(buildingId: String): List<Contract> {
        if (buildingId.isBlank()) {
            Log.d("ContractRepository", "Query skipped: buildingId is blank")
            return emptyList()
        }
        Log.d("ContractRepository", "Querying for buildingId: '$buildingId'")
        return try {
            val querySnapshot = collection.where {
                "buildingId" equalTo buildingId
            }.get()
            Log.d("ContractRepository", "Found ${querySnapshot.documents.size} documents for buildingId: '$buildingId'")
            querySnapshot.documents.map { doc ->
                val contract = doc.data<Contract>()
                Log.d("ContractRepository", "Fetched contract: ${contract.contractNumber}, buildingId: ${contract.buildingId}")
                contract.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("ContractRepository", "Error fetching contracts by building ID", e)
            emptyList()
        }
    }
    suspend fun getContractsByRoomId(roomId: String): List<Contract> {
        if (roomId.isBlank()) {
            Log.d("ContractRepository", "RoomId is blank")
            return emptyList()
        }
        return try {
            val querySnapshot = collection.where {
                "roomId" equalTo roomId
            }.get()
            querySnapshot.documents.map { doc ->
                val contract = doc.data<Contract>()
                contract.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("ContractRepository", "Error fetching contracts by room ID", e)
            emptyList()
        }
    }

    // get the number of tenants in a building that have a contract
    suspend fun getTenantsByBuildingId(buildingId: String): List<String> {
        if (buildingId.isBlank()) return emptyList()

        return try {
            val contracts = getContractsByBuildingId(buildingId)
            Log.d("ContractRepository", "Found ${contracts.size} contracts for buildingId: '$buildingId'")
            contracts.flatMap { it.tenantIds }.distinct()
        } catch (e: Exception) {
            Log.e("ContractRepository", "Error fetching tenant IDs by building ID", e)
            emptyList()
        }
    }
    suspend fun getTenantsByRoomId(roomId : String) : List<String> {
        if (roomId.isBlank()) {
            Log.d("ContractRepository", "RoomId is blank")
            return emptyList()
        }
        return try {
            val contracts = getContractsByRoomId(roomId)
            Log.d("ContractRepository", "Found ${contracts.size} contracts for roomId: '$roomId'")
            contracts.flatMap { it.tenantIds }.distinct()
        } catch (e : Exception) {
            Log.e("ContractRepository", "Error fetching tenant IDs by room ID", e)
            emptyList()
        }
    }
}