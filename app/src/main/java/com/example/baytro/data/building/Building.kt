package com.example.baytro.data

import androidx.compose.runtime.Stable
import com.example.baytro.data.service.Service
import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

enum class BuildingStatus {
    ACTIVE,
    INACTIVE,
    ARCHIVED
}

@Stable
@Serializable
data class Building(
    @kotlinx.serialization.Transient
    @DocumentId val id: String = "",
    val name: String,
    val floor: Int,
    val address: String,
    val status: BuildingStatus,
    val billingDate: Int,
    val paymentStart: Int,
    val paymentDue: Int,
    val imageUrls: List<String> = emptyList(),
    val userId: String = "",
    val services: List<Service> = emptyList(),
)

@Stable
@Serializable
data class BuildingSummary(
    @kotlinx.serialization.Transient
    @DocumentId
    val id: String = "",
    val name: String = "",
    val pendingCount: Int = 0
)

fun Building.toSummary(): BuildingSummary {
    return BuildingSummary(
        id = this.id,
        name = this.name
    )
}

@Serializable
data class BuildingWithStats(
    val building: Building,
    val occupiedRooms: Int = 0,
    val totalRooms: Int = 0,
    val revenue: Double = 0.0
)

@Serializable
data class BuildingListResponse(
    val buildings: List<BuildingWithStats> = emptyList()
)