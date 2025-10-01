package com.example.baytro.data

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
enum class Status {
    AVAILABLE, RENTED, UNDER_MAINTENANCE
}
@Serializable
enum class Furniture {
    Furnished,
    Unfurnished,
}

@Serializable
data class Floor(val number: Int, val rooms: List<Room>)

@Serializable
data class Room (
    @kotlinx.serialization.Transient
    @DocumentId
    val id : String = "",
    val buildingName : String,
    val floor: Int,
    val roomNumber : Int,
    val size : Int,
    val status: Status,
    val rentalFee : Int,
    val interior : Furniture,
    val note : String = ""
)