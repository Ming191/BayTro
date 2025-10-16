package com.example.baytro.data.billing

import kotlinx.serialization.Serializable

// Represents a single line item within a bill (e.g., Rent, Electricity).
@Serializable
data class BillLineItem(
    val description: String = "",
    val totalCost: Double = 0.0,
    val quantity: Int? = null,
    val pricePerUnit: Double? = null,
    val readingId: String? = null,
    val serviceName: String? = null
)