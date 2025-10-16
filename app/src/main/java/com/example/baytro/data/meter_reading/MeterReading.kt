package com.example.baytro.data.meter_reading

import com.example.baytro.data.MeterStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MeterReading(
    @Transient
    @SerialName("id")
    val id: String = "",
    @SerialName("contractId")
    val contractId: String = "",
    @SerialName("roomId")
    val roomId: String = "",
    @SerialName("landlordId")
    val landlordId: String = "",
    @SerialName("tenantId")
    val tenantId: String = "",
    @SerialName("status")
    val status: MeterStatus = MeterStatus.PENDING,
    @SerialName("createdAt")
    val createdAt: Long = 0,
    @SerialName("approvedAt")
    val approvedAt: String? = null,
    @SerialName("declinedAt")
    val declinedAt: String? = null,
    @SerialName("declineReason")
    val declineReason: String? = null,


    @SerialName("electricityValue")
    val electricityValue: Int = 0,
    @SerialName("waterValue")
    val waterValue: Int = 0,

    @SerialName("electricityImageUrl")
    val electricityImageUrl: String? = null,
    @SerialName("waterImageUrl")
    val waterImageUrl: String? = null,

    @SerialName("electricityConsumption")
    val electricityConsumption: Int? = null,
    @SerialName("waterConsumption")
    val waterConsumption: Int? = null,

    @SerialName("electricityCost")
    val electricityCost: Double? = null,
    @SerialName("waterCost")
    val waterCost: Double? = null,

    @SerialName("totalCost")
    val totalCost: Double? = null
)