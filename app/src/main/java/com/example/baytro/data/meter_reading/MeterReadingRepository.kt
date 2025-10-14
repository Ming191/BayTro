package com.example.baytro.data.meter_reading

import com.example.baytro.data.MeterStatus
import com.example.baytro.data.Repository
import com.example.baytro.data.contract.Status
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MeterReadingRepository(
    private val db: FirebaseFirestore
) : Repository<MeterReading> {
    private val collection = db.collection("meter_readings")

    override suspend fun getAll(): List<MeterReading> {
        val snapshot = collection.get()
        return snapshot.documents.map {
            it.data<MeterReading>().copy(id = it.id)
        }
    }

    override suspend fun getById(id: String): MeterReading? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            snapshot.data<MeterReading>().copy(id = snapshot.id)
        } else {
            null
        }
    }

    override suspend fun add(item: MeterReading): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: MeterReading) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: MeterReading) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }

    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    // Get readings by contract ID
    suspend fun getByContractId(contractId: String): List<MeterReading> {
        val snapshot = collection
            .where { "contractId" equalTo contractId }
            .orderBy("createdAt", Direction.DESCENDING)
            .get()
        return snapshot.documents.map {
            it.data<MeterReading>().copy(id = it.id)
        }
    }

    // Listen for pending readings for landlord
    fun listenForPendingReadings(landlordId: String): Flow<List<MeterReading>> {
        return collection
            .where { "landlordId" equalTo landlordId }
            .where { "status" equalTo MeterStatus.METER_PENDING }
            .orderBy("createdAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.data<MeterReading>().copy(id = doc.id)
                }
            }
    }

    // Listen for readings by contract
    fun listenForReadingsByContract(contractId: String): Flow<List<MeterReading>> {
        return collection
            .where { "contractId" equalTo contractId }
            .orderBy("createdAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.data<MeterReading>().copy(id = doc.id)
                }
            }
    }

    // Get readings by tenant
    suspend fun getByTenantId(tenantId: String): List<MeterReading> {
        val snapshot = collection
            .where { "tenantId" equalTo tenantId }
            .orderBy("createdAt", Direction.DESCENDING)
            .get()
        return snapshot.documents.map {
            it.data<MeterReading>().copy(id = it.id)
        }
    }
}
