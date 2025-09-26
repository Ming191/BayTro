package com.example.baytro.viewModel.contract

import com.example.baytro.data.contract.Status


data class AddContractFormState(
    val propertyId: String = "",
    val roomId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val rentalFee: String = "",
    val deposit: String = "",
    val status: Status,
    val photosURL: List<String> = emptyList(),
)