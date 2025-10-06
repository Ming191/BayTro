package com.example.baytro.data.service

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

enum class Metric {
    ROOM,
    PERSON,
    KWH,
    M3,
    OTHER
}

enum class Status {
    ACTIVE,
    INACTIVE
}

@Serializable
data class Service(
    @kotlinx.serialization.Transient
    @DocumentId
    val id: String = "",
    val name: String = "",
    val price: String = "",
    val metric: Metric = Metric.OTHER,
    val status: Status = Status.ACTIVE,
)
