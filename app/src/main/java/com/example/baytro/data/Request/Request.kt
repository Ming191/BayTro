package com.example.baytro.data.Request

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
    val status: RequestStatus,
    val createdAt: String,
    val scheduledDate: String,
    val imageUrls: List<String>,
    val description: String,
    val title: String,
)
