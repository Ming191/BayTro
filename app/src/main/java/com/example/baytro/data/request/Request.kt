package com.example.baytro.data.request

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

enum class RequestStatus {
    PENDING,
    IN_PROGRESS,
    DONE
}

@Serializable
data class Request(
    @kotlinx.serialization.Transient
    val id: String = "",
    val tenantId: String = "",
    val roomId: String = "",
    val landlordId: String = "",
    val status: RequestStatus,
    val createdAt: Timestamp,
    val scheduledDate: String,
    val imageUrls: List<String>,
    val description: String,
    val title: String,

    val assigneeName: String?,
    val completionDate: String?,
    val acceptedDate: String?,
    val assigneePhoneNumber: String?,
)

data class FullRequestInfo(
    val request: Request,
    val tenantName: String,
    val tenantPhoneNumber: String,
    val roomName: String,
    val buildingName: String,
    val landlordName: String,
    val landlordPhoneNumber: String
)
