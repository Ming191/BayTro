package com.example.baytro.data

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
data class Building(
    @kotlinx.serialization.Transient
    @DocumentId val id: String = "",

    val name: String,
    val floor: Int,
    val address: String,
    val status: String,
    val billingDate: Int,
    val paymentStart: Int,
    val paymentDue: Int,
    val userId: String = "", // ID of landlord owner
)
