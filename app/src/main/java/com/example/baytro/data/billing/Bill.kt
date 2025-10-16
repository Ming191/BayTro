package com.example.baytro.data.billing

import com.google.firebase.firestore.DocumentId
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

// Represents the full, detailed bill document.
@Serializable
data class Bill(
    @kotlinx.serialization.Transient
    @DocumentId
    val id: String = "",
    val contractId: String = "",
    val roomName: String = "", // Denormalized for easy display
    val buildingName: String = "",
    val tenantName: String = "", // Denormalized for easy display
    val totalAmount: Double = 0.0,
    val status: BillStatus = BillStatus.NOT_ISSUED_YET,
    val issuedDate: Timestamp = Timestamp.now(), // Timestamp
    val paymentDueDate: Timestamp = Timestamp.now(), // Timestamp
    val lineItems: List<BillLineItem> = emptyList(),
    val paymentInfo: String = "", // Landlord's bank info, QR code URL, etc.
    val landlordId: String = "",
    val tenantId: String = "",
    val buildingId: String = "",
    val roomId: String = "",
    val month: Int = 0, // 1-12
    val year: Int = 0 // e.g., 2025
)

// A summarized version for list views.
@Serializable
data class BillSummary(
    val id: String = "",
    val roomName: String = "",
    val buildingName: String = "",
    val totalAmount: Double = 0.0,
    val status: BillStatus = BillStatus.NOT_ISSUED_YET,
    val month: Int = 0,
    val year: Int = 0,
    val paymentDueDate: Timestamp = Timestamp.now() // Thêm trường này rất hữu ích
)

fun Bill.toSummary(buildingNameOverride: String? = null): BillSummary {
    return BillSummary(
        id = this.id,
        roomName = this.roomName,
        buildingName = buildingNameOverride ?: this.buildingName,
        totalAmount = this.totalAmount,
        status = this.status,
        month = this.month,
        year = this.year,
        paymentDueDate = this.paymentDueDate
    )
}

