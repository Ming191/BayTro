package com.example.baytro.data

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Room(
    @Transient
    @DocumentId
    val id: String = "",
    val buildingId: String,
    val roomNumber: String,
)