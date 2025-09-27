package com.example.baytro.data.contract

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
enum class Status {
    ACTIVE,
    OVERDUE,
    TERMINATED
}

@Serializable
data class Contract(
    @Transient
    @DocumentId
    val id: String,
    val tenantId: String,
    val roomId: String,
    val startDate: Int,
    val endDate: Int,
    val rentalFee: Int,
    val deposit: Int,
    val status: Status,
    val photosURL: List<String> = emptyList(),
)