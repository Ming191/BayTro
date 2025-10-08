package com.example.baytro.viewModel.contract

import android.net.Uri
import com.example.baytro.data.Building
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.Room
import com.example.baytro.utils.ValidationResult

data class EditContractFormState(
    val contractId: String = "",
    val selectedBuilding: Building? = null,
    val selectedRoom: Room? = null,
    val startDate: String = "",
    val endDate: String = "",
    val rentalFee: String = "",
    val deposit: String = "",
    val status: Status = Status.PENDING,
    val existingPhotosURL: List<String> = emptyList(),
    val selectedPhotos: List<Uri> = emptyList(),
    val tenantList: List<String> = emptyList(),
    val contractNumber: String = "",

    val startDateError: ValidationResult = ValidationResult.Success,
    val endDateError: ValidationResult = ValidationResult.Success,
    val rentalFeeError: ValidationResult = ValidationResult.Success,
    val depositError: ValidationResult = ValidationResult.Success,
)

