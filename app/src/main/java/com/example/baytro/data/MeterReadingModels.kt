package com.example.baytro.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MeterStatus {
    METER_PENDING,
    METER_APPROVED,
    METER_DECLINED
}

@Serializable
data class MeterReadingResponse(
    @SerialName("text")
    val text: String,
    @SerialName("detections")
    val detections: List<MeterDetection>
)

@Serializable
data class MeterDetection(
    @SerialName("label")
    val label: String,
    @SerialName("x")
    val x: Float,
    @SerialName("conf")
    val conf: Float
)

