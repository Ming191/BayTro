package com.example.baytro.data.meter_reading

import com.example.baytro.data.MeterStatus
import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MeterReadingRepository(
    db: FirebaseFirestore
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

    fun listenForPendingReadingsByBuilding(
        landlordId: String,
        buildingId: String
    ): Flow<List<MeterReading>> {
        return collection
            .where {
                all(
                    "landlordId" equalTo landlordId,
                    "buildingId" equalTo buildingId,
                    "status" equalTo MeterStatus.PENDING
                )
            }
            .orderBy("createdAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.data<MeterReading>().copy(id = doc.id)
                }
            }
    }

    fun listenForReadingsByStatus(
        landlordId: String,
        status: MeterStatus
    ): Flow<List<MeterReading>> {
        return collection
            .where {
                all(
                    "landlordId" equalTo landlordId,
                    "status" equalTo status
                )
            }
            .orderBy("createdAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.data<MeterReading>().copy(id = doc.id)
                }
            }
    }

    suspend fun getReadingsByContractPaginated(
        contractId: String,
        pageSize: Long,
        startAfterTimestamp: Timestamp? = null
    ): List<MeterReading> {
        var query = collection
            .where { "contractId" equalTo contractId }
            .orderBy("createdAt", Direction.DESCENDING)
            .limit(pageSize)

        startAfterTimestamp?.let {
            query = collection
                .where { "contractId" equalTo contractId }
                .orderBy("createdAt", Direction.DESCENDING)
                .where { "createdAt" lessThan it }
                .limit(pageSize)
        }

        val snapshot = query.get()
        return snapshot.documents.map {
            it.data<MeterReading>().copy(id = it.id)
        }
    }
}
