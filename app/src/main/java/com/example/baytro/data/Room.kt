package com.example.baytro.data

import kotlinx.serialization.Serializable

@Serializable
enum class Status {
    AVAILABLE, RENTED, UNDER_MAINTENANCE
}

@Serializable
data class Room (
    @Transient
    val id : String,
    val buildingName : String,
    val floor: Int,
    val roomNumber : Int,
    val size : Int,
    val status: Status,
    val rentalFee : Int,
    val interior : Boolean,
    val note : String = ""
)