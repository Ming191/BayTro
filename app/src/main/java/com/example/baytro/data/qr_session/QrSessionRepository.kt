package com.example.baytro.data.qr_session

import android.util.Log
import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map


data class PendingQrSession(
    val sessionId: String,
    val tenantId: String,
    val tenantName: String,
    val tenantAvatarUrl: String
)


class QrSessionRepository(
    db: FirebaseFirestore,
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
                    "status" equalTo QrSessionStatus.SCANNED
                )
            }.get()
            Log.d("QrSessionRepository", "scanned: ${snapshot.documents.size}")
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            Log.e("QrSessionRepository", "Error checking scanned sessions", e)
            false
        }
    }

    fun listenForScannedSessions(contractId: String): Flow<List<PendingQrSession>> {
        if (contractId.isBlank()) {
            Log.w("QrSessionRepository", "Contract ID is blank, returning empty list")
            return flowOf(emptyList())
        }

        val query = collection.where {
            all(
                "contractId" equalTo contractId,
                "status" equalTo QrSessionStatus.SCANNED
            )
        }

        return query.snapshots
            .map { querySnapshot ->
                coroutineScope {
                    val deferreds = querySnapshot.documents.map { doc ->
                        async {
                            try {
                                val sessionData = doc.data<QrSession>()
                                val tenantId = sessionData.scannedByTenantId

                                if (tenantId != null) {
                                    val user = userRepository.getById(tenantId)
                                    user?.let {
                                        PendingQrSession(
                                            sessionId = doc.id,
                                            tenantId = tenantId,
                                            tenantName = it.fullName,
                                            tenantAvatarUrl = it.profileImgUrl ?: ""
                                        )
                                    }
                                } else {
                                    Log.w("QrSessionRepository", "Session ${doc.id} has no tenantId")
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e("QrSessionRepository", "Error processing session ${doc.id}", e)
                                null
                            }
                        }
                    }

                    deferreds.awaitAll().filterNotNull()
                }
            }
            .catch { e ->
                Log.e("QrSessionRepository", "Error in listenForScannedSessions flow", e)
                emit(emptyList())
            }
    }

    fun listenForSessionApproval(tenantId: String): Flow<String?> {
        if (tenantId.isBlank()) {
            Log.w("QrSessionRepository", "Tenant ID is blank, returning empty flow")
            return flowOf(null)
        }

        val query = collection.where {
            all(
                "scannedByTenantId" equalTo tenantId,
                "status" equalTo QrSessionStatus.SCANNED
            )
        }

        return query.snapshots
            .map { querySnapshot ->
                Log.d("QrSessionRepository", "Session snapshot received for tenant $tenantId, docs: ${querySnapshot.documents.size}")
                if (querySnapshot.documents.isEmpty()) {
                    Log.d("QrSessionRepository", "No SCANNED sessions found, checking for CONFIRMED session")
                    val confirmedSnapshot = collection.where {
                        all(
                            "scannedByTenantId" equalTo tenantId,
                            "status" equalTo QrSessionStatus.CONFIRMED
                        )
                    }.get()

                    if (confirmedSnapshot.documents.isNotEmpty()) {
                        val session = confirmedSnapshot.documents.first().data<QrSession>()
                        Log.d("QrSessionRepository", "Session CONFIRMED! Contract ID: ${session.contractId}")
                        session.contractId
                    } else {
                        Log.d("QrSessionRepository", "No CONFIRMED session found either")
                        null
                    }
                } else {
                    Log.d("QrSessionRepository", "SCANNED session still exists, waiting for landlord approval")
                    null
                }
            }
            .catch { e ->
                Log.e("QrSessionRepository", "Error listening for session approval", e)
                emit(null)
            }
    }
}
