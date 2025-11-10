package com.example.baytro.data.request

import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.FirebaseFirestore

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
}
