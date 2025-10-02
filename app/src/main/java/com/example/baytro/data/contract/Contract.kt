package com.example.baytro.data.contract

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
enum class Status {
    ACTIVE,
    OVERDUE,
    PENDING,
    ENDED
}

@Serializable
data class Contract(
    @kotlinx.serialization.Transient
    @DocumentId val id: String = "",
    val contractNumber: String,
    val landlordId: String,
    val tenantIds: List<String>,
    val roomId: String,
    val roomNumber: String,
    val buildingId: String,
    val startDate: String,
    val endDate: String,
    val rentalFee: Int,
    val deposit: Int,
    val status: Status,
    val photosURL: List<String> = emptyList(),
)