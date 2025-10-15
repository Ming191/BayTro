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
    @SerialName("type")
    val type: MeterType = MeterType.ELECTRICITY,
    @SerialName("value")
    val value: Int = 0,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("status")
    val status: MeterStatus = MeterStatus.PENDING,
    @SerialName("createdAt")
    val createdAt: Long = 0,
    @SerialName("approvedAt")
    val approvedAt: Long? = null,
    @SerialName("declinedAt")
    val declinedAt: Long? = null,
    @SerialName("consumption")
    val consumption: Int? = null,
    @SerialName("cost")
    val cost: Double? = null,
    @SerialName("declineReason")
    val declineReason: String? = null
)

@Serializable
enum class MeterType {
    @SerialName("electricity")
    ELECTRICITY,
    @SerialName("water")
    WATER
}

