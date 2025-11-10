package com.example.baytro.data

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
    val text: String,
    val detections: List<MeterDetection>
)

@Serializable
data class MeterDetection(
    val label: String,
    val x: Float,
    val conf: Float
)

