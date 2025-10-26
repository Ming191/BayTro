package com.example.baytro.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
enum class MeterStatus {
    PENDING,
    APPROVED,
    DECLINED,
    CANCELLED
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

