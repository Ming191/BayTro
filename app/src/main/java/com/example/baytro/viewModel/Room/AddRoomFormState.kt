package com.example.baytro.viewModel.Room

import com.example.baytro.data.Furniture

data class AddRoomFormState (
    val buildingName : String = "",
    val roomNumber: String = "",
    val floor : String = "",
    val size : String = "",
    val rentalFee : String = "",
    val interior : Furniture = Furniture.Furnished,
)