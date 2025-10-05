package com.example.baytro.data

import com.example.baytro.data.service.Service
import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
data class Building(
    @DocumentId val id: String = "",
    val name: String,
    val floor: Int,
    val address: String,
    val status: String,
    val billingDate: Int,
    val paymentStart: Int,
    val paymentDue: Int,
    val imageUrls: List<String> = emptyList(),
    val userId: String = "",
    val services: List<Service> = emptyList(),
)