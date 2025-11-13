package com.example.baytro.data.qr_session

import com.google.firebase.firestore.DocumentId
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
enum class QrSessionStatus {
    PENDING,
    SCANNED,
    CONFIRMED,
    DECLINED,
    EXPIRED
}

@Serializable
data class QrSession(
    @kotlinx.serialization.Transient
    @DocumentId
    val id: String = "",
    val contractId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val inviterId: String = "",
    val scannedAt: Timestamp? = null,
    val scannedByTenantId: String? = null,
    val status: QrSessionStatus = QrSessionStatus.PENDING,
)
