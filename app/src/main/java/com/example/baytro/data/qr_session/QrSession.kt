package com.example.baytro.data.qr_session

import com.google.firebase.firestore.DocumentId
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class QrSessionStatus {
    @SerialName("pending")
    PENDING,
    @SerialName("scanned")
    SCANNED,
    @SerialName("approved")
    APPROVED,
    @SerialName("rejected")
    REJECTED,
    @SerialName("expired")
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
