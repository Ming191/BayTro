package com.example.baytro.data.room

import com.example.baytro.data.service.Service
import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
enum class Status {
    Available,
    Occupied,
    Maintenance,
}

@Serializable
enum class Furniture {
    Furnished,
    Unfurnished,
    Unknow
}

@Serializable
data class Floor(val number: Int, val rooms: List<Room>)

@Serializable
data class Room(
    @kotlinx.serialization.Transient
    @DocumentId
    val id: String = "",
    val buildingId : String,
    val floor: Int,
    val roomNumber : String,
    val size : Int,
    val status: Status,
    val rentalFee : Int,
    val interior : Furniture,
    val extraService: List<Service> = emptyList()
)