package com.example.baytro.viewModel.Room

data class AddRoomFormState (
    val buildingName : String = "",
    val roomNumber: Int = -1,
    val floor : Int = -1,
    val size :Int = -1,
    val rentalFee : Int = -1,
    val interior : Boolean = false
)