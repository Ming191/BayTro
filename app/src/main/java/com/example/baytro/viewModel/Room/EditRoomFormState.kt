package com.example.baytro.viewModel.Room

import com.example.baytro.data.room.Furniture
import com.example.baytro.data.service.Service

data class EditRoomFormState(
    val buildingName: String = "",
    val buildingNameError: String? = null,

    val roomNumber: String = "",
    val roomNumberError: String? = null,

    val floor: String = "",
    val floorError: String? = null,

    val size: String = "",
    val sizeError: String? = null,

    val rentalFeeUI: String = "",
    val rentalFee: String = "",
    val rentalFeeError: String? = null,

    val interior: Furniture = Furniture.UNFURNISHED,
    val interiorError: String? = null,

    val extraService: List<Service> = emptyList(),
    val extraServiceError: String? = null
)
