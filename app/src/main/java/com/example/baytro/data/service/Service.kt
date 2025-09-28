package com.example.baytro.data.service

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
enum class Status {
    ACTIVE,
    UNACTIVE
}

@Serializable
data class Service(
    @kotlinx.serialization.Transient
    @DocumentId
    val id: String = "",
    val buildingId: String,
    val roomId: String,
    val pricing_type: String,
    val price_per_unit: String,
    val created_at: String,
    val updated_at: String,
    val status: Status,
)