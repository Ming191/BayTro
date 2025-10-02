package com.example.baytro.viewModel.contract

import com.example.baytro.data.contract.Status
import com.example.baytro.data.user.User
import java.text.NumberFormat
import java.util.Locale

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
) {
    val formattedRentalFee: String
        get() = formatCurrency(rentalFee)

    val formattedDeposit: String
        get() = formatCurrency(deposit)

    val tenantCount: Int
        get() = tenantList.size

    val hasActiveTenants: Boolean
        get() = tenantList.isNotEmpty()

    val isActiveContract: Boolean
        get() = status == Status.ACTIVE

    val isPendingContract: Boolean
        get() = status == Status.PENDING

    private fun formatCurrency(amount: String): String {
        return try {
            val numericAmount = amount.toDoubleOrNull() ?: 0.0
            val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
            formatter.format(numericAmount)
        } catch (e: Exception) {
            amount
        }
    }
}
