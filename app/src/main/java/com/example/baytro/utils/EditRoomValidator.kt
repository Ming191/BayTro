package com.example.baytro.utils

import com.example.baytro.data.room.Furniture
import com.example.baytro.data.room.Room

object EditRoomValidator {

    fun validateRoomNumber(
        roomNumber: String,
        floorNumber : String,
        existingRooms : List<Room>,
        currentRoomId: String? = null
    ): String? {
        val existingRoom = existingRooms.find {
            it.roomNumber == roomNumber && it.id != currentRoomId
        }
        if (existingRoom != null) {
            return "Room number already exists"
        }
        return if (roomNumber.isBlank()) "Room number is required"
        else if (!roomNumber.matches(Regex("\\d+"))) "Room number must be a positive integer"
        else if (roomNumber.substring(0, floorNumber.length) != floorNumber) "Room number must be in the same floor"
        else null
    }

    fun validateFloor(floor: String): String? {
        return if (floor.isBlank()) "Floor is required"
        else if (!floor.matches(Regex("\\d+"))) "Floor number must be a positive integer"
        else null
    }

    fun validateSize(size: String): String? {
        return if (size.isBlank()) "Size is required"
        else if (!size.matches(Regex("\\d+"))) "Size must be a positive integer"
        else null
    }

    fun validateRentalFee(rentalFee: String): String? {
        return if (rentalFee.isBlank()) "Rental fee is required"
        else if (!rentalFee.matches(Regex("\\d+"))) "Rental fee must be a positive integer"
        else null
    }

    fun validateInterior(interior: Furniture?): String? {
        return if (interior == null) "Interior is required" else null
    }
}
