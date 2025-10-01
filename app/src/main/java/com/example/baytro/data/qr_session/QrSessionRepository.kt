package com.example.baytro.data.qr_session

import android.util.Log
import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map


data class PendingQrSession(
    val sessionId: String,
    val tenantId: String,
    val tenantName: String,
    val tenantAvatarUrl: String
)


class QrSessionRepository(
    private val db: FirebaseFirestore,
    private val userRepository: com.example.baytro.data.user.UserRepository
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

    fun listenForScannedSessions(contractId: String): Flow<List<PendingQrSession>> {
        if (contractId.isBlank()) {
            return flowOf(emptyList())
        }

        val query = collection.where {
            all(
                "contractId" equalTo contractId,
                "status" equalTo "scanned"
            )
        }

        return query.snapshots
            .map { querySnapshot ->
                coroutineScope {
                    val deferreds = querySnapshot.documents.map { doc ->
                        async {
                            val sessionData = doc.data<QrSession>()
                            val tenantId = sessionData.scannedByTenantId

                            if (tenantId != null) {
                                val user = userRepository.getById(tenantId)
                                user?.let {
                                    PendingQrSession(
                                        sessionId = doc.id,
                                        tenantId = tenantId,
                                        tenantName = it.fullName,
                                        tenantAvatarUrl = it.profileImgUrl!!
                                    )
                                }
                            } else {
                                null
                            }
                        }
                    }

                    deferreds.awaitAll().filterNotNull()
                }
            }
    }
}
