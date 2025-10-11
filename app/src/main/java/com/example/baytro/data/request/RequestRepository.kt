package com.example.baytro.data.request

import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RequestRepository(
    db: FirebaseFirestore
) : Repository<Request> {
    private val collection = db.collection("requests")

    override suspend fun getAll(): List<Request> {
        val snapshot = collection.get()
        return snapshot.documents.map { doc ->
            val request = doc.data<Request>()
            request.copy(id = doc.id)
        }
    }

    override suspend fun getById(id: String): Request? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val request = snapshot.data<Request>()
            request.copy(id = snapshot.id)
        } else null
    }

    override suspend fun add(item: Request): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: Request) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: Request) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }

    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    // Get all requests as Flow for real-time updates
    fun getAllRequestsFlow(): Flow<List<Request>> {
        return collection.snapshots.map { snapshot ->
            snapshot.documents.map { doc ->
                val request = doc.data<Request>()
                request.copy(id = doc.id)
            }
        }
    }

    // Get requests by roomId
    suspend fun getRequestsByRoomId(roomId: String): List<Request> {
        val snapshot = collection.where { "roomId" equalTo roomId }.get()
        return snapshot.documents.map { doc ->
            val request = doc.data<Request>()
            request.copy(id = doc.id)
        }
    }

    // Get requests by roomIds (for landlord filtering by building)
    suspend fun getRequestsByRoomIds(roomIds: List<String>): List<Request> {
        if (roomIds.isEmpty()) return emptyList()

        val snapshot = collection.where { "roomId" inArray roomIds }.get()
        return snapshot.documents.map { doc ->
            val request = doc.data<Request>()
            request.copy(id = doc.id)
        }
    }

    // Get requests by roomIds as Flow
    fun getRequestsByRoomIdsFlow(roomIds: List<String>): Flow<List<Request>> {
        if (roomIds.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        return collection.where { "roomId" inArray roomIds }.snapshots.map { snapshot ->
            snapshot.documents.map { doc ->
                val request = doc.data<Request>()
                request.copy(id = doc.id)
            }
        }
    }

    // Get requests by tenantId
    suspend fun getRequestsByTenantId(tenantId: String): List<Request> {
        val snapshot = collection.where { "tenantId" equalTo tenantId }.get()
        return snapshot.documents.map { doc ->
            val request = doc.data<Request>()
            request.copy(id = doc.id)
        }
    }
}
