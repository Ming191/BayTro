package com.example.baytro.data.qr_session

import android.util.Log
import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.FirebaseFirestore

class QrSessionRepository(
    private val db: FirebaseFirestore
) : Repository<QrSession> {
    private val collection = db.collection("qr_sessions")

    override suspend fun getAll(): List<QrSession> {
        val snapshot = collection.get()
        return snapshot.documents.map {
            it.data<QrSession>().copy(id = it.id)
        }
    }

    override suspend fun getById(id: String): QrSession? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val session = snapshot.data<QrSession>()
            session.copy(id = snapshot.id)
        } else {
            null
        }
    }

    override suspend fun add(item: QrSession): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: QrSession) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: QrSession) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }

    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    suspend fun hasScannedSession(scannedByTenantId: String): Boolean {
        return try {
            val snapshot = collection.where {
                all(
                    "scannedByTenantId" equalTo scannedByTenantId,
                    "status" equalTo "scanned"
                )
            }.get()
            Log.d("QrSessionRepository", "scanned: ${snapshot.documents.size}")
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
