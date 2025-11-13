package com.example.baytro.viewModel.contract

import android.net.Uri
import com.example.baytro.data.Building
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.Room
import com.example.baytro.utils.ValidationResult


data class AddContractFormState(
    val startDate: String = "",
    val endDate: String = "",
    val rentalFee: String = "",
    val deposit: String = "",
    val status: Status = Status.PENDING,
    val photosURL: List<String> = emptyList(),
    val availableBuildings: Building? = null,
    val availableRooms: Room? = null,
    val selectedPhotos: List<Uri> = emptyList(),
    val initialElectricityReading: String = "",
    val initialWaterReading: String = "",
    val initialMeterPhotos: List<Uri> = emptyList(),

    val buildingIdError: ValidationResult = ValidationResult.Success,
    val roomIdError: ValidationResult = ValidationResult.Success,
    val startDateError: ValidationResult = ValidationResult.Success,
    val endDateError: ValidationResult = ValidationResult.Success,
    val rentalFeeError: ValidationResult = ValidationResult.Success,
    val depositError: ValidationResult = ValidationResult.Success,
    val initialElectricityError: ValidationResult = ValidationResult.Success,
    val initialWaterError: ValidationResult = ValidationResult.Success,
)