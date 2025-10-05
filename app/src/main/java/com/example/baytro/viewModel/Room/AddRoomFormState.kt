package com.example.baytro.viewModel.Room

import com.example.baytro.data.Furniture

data class AddRoomFormState(
    val buildingName: String = "",
    //val buildingNameError: String? = null,

    val roomNumber: String = "",
    val roomNumberError: String? = null,

    val floor: String = "",
    val floorError: String? = null,

    val size: String = "",
    val sizeError: String? = null,

    val rentalFee: String = "",
    val rentalFeeError: String? = null,

    val interior: Furniture = Furniture.Unknow,
    val interiorError: String? = null
)
