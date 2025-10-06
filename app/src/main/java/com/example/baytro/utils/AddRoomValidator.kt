package com.example.baytro.utils

import com.example.baytro.data.room.Furniture

object AddRoomValidator {
    fun validateBuildingName(buildingName: String): String? {
        return if (buildingName.isBlank()) "Building name is required" else null
    }

    fun validateRoomNumber(roomNumber: String): String? {
        return if (roomNumber.isBlank()) "Room number is required" else null
    }

    fun validateFloor(floor: String): String? {
        return if (floor.isBlank()) "Floor is required" else null
    }

    fun validateSize(size: String): String? {
        return if (size.isBlank()) "Size is required" else null
    }

    fun validateRentalFee(rentalFee: String): String? {
        return if (rentalFee.isBlank()) "Rental fee is required" else null
    }

    fun validateInterior(interior: Furniture?): String? {
        return if (interior == null) "Interior is required" else null
    }
}
