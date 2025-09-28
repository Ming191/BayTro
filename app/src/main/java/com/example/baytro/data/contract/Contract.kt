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
    @kotlinx.serialization.Transient
    @DocumentId
    val id: String = "",
    val tenantId: String,
    val roomId: String,
    val startDate: String,
    val endDate: String,
    val rentalFee: Int,
    val deposit: Int,
    val status: Status,
    val photosURL: List<String> = emptyList(),
)