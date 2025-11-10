package com.example.baytro.data.meter_reading

import com.example.baytro.data.MeterStatus
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class MeterReading(
    @kotlinx.serialization.Transient
    val id: String = "",
    val contractId: String = "",
    val roomId: String = "",
    val buildingId: String = "",
    val landlordId: String = "",
    val tenantId: String = "",
    val status: MeterStatus = MeterStatus.PENDING,
    val createdAt: Timestamp? = null,
    val approvedAt: Timestamp? = null,
    val declinedAt: Timestamp? = null,
    val declineReason: String? = null,


    val electricityValue: Int = 0,
    val waterValue: Int = 0,

    val electricityImageUrl: String? = null,
    val waterImageUrl: String? = null,

    val electricityConsumption: Int? = null,
    val waterConsumption: Int? = null,

    val electricityCost: Double? = null,
    val waterCost: Double? = null,
    val totalCost: Double? = null,

    val roomName: String = "Unknown Room",
    val buildingName: String = "Unknown Building",
)