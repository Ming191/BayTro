package com.example.baytro.data.billing

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethod {
    BANK_TRANSFER,
    CASH,
    OTHER
}

@Serializable
data class Bill(
    val id: String = "",
    val contractId: String = "",
    val landlordId: String = "",
    val buildingId: String = "",
    val roomId: String = "",
    val tenantIds: List<String> = emptyList(),

    val roomName: String = "",
    val buildingName: String = "",
    val tenantName: String = "",

    val totalAmount: Double = 0.0,
    val lineItems: List<BillLineItem> = emptyList(),

    val status: BillStatus = BillStatus.NOT_ISSUED_YET,
    val month: Int = 0,
    val year: Int = 0,
    val issuedDate: Timestamp,
    val paymentDueDate: Timestamp,

    val paymentCode: String? = null,
    val paymentInfo: String = "",

    // Các trường được điền bởi webhook
    val paymentDate: Timestamp? = null,
    val paidAmount: Double? = null,
    val sepayTransactionId: Long? = null,
    val paidBy_BankHolderName: String? = null,

    val paymentMethod: PaymentMethod? = PaymentMethod.OTHER
)

@Serializable
data class BillSummary(
    val id: String = "",
    val roomName: String = "",
    val buildingName: String = "",
    val totalAmount: Double = 0.0,
    val status: BillStatus = BillStatus.NOT_ISSUED_YET,
    val month: Int = 0,
    val year: Int = 0,
    val paymentDueDate: Timestamp
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