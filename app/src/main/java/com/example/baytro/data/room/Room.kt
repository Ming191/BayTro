package com.example.baytro.data.room

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
data class Room(
    @kotlinx.serialization.Transient
    @DocumentId
    val id: String = "",
    val buildingId: String,
    val roomNumber: String,
)