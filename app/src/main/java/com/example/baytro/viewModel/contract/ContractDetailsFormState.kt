package com.example.baytro.viewModel.contract

import com.example.baytro.data.contract.Status
import com.example.baytro.data.user.User

data class ContractDetailsFormState(
    val contractNumber: String = "",
    val status: Status = Status.PENDING,
    val roomNumber: String = "",
    val buildingName: String = "",
    val tenantList: List<User> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
    val rentalFee: String = "",
    val deposit: String = "",
)