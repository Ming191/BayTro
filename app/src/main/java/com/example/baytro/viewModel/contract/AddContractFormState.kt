package com.example.baytro.viewModel.contract

import com.example.baytro.data.Building
import com.example.baytro.data.Room
import com.example.baytro.data.contract.Status
import com.example.baytro.utils.ValidationResult


data class AddContractFormState(
    val selectedBuilding: Building? = null,
    val selectedRoom: Room? = null,
    val startDate: String = "",
    val endDate: String = "",
    val rentalFee: String = "",
    val deposit: String = "",
    val status: Status = Status.entries[0],
    val photosURL: List<String> = emptyList(),
    val availableBuildings: List<Building> = emptyList(),
    val availableRooms: List<Room> = emptyList(),

    val buildingIdError: ValidationResult = ValidationResult.Success,
    val roomIdError: ValidationResult = ValidationResult.Success,
    val startDateError: ValidationResult = ValidationResult.Success,
    val endDateError: ValidationResult = ValidationResult.Success,
    val rentalFeeError: ValidationResult = ValidationResult.Success,
    val depositError: ValidationResult = ValidationResult.Success,
    val statusError: ValidationResult = ValidationResult.Success,
    val photosURLError: ValidationResult = ValidationResult.Success,
    val startEndDateError: ValidationResult = ValidationResult.Success,
)