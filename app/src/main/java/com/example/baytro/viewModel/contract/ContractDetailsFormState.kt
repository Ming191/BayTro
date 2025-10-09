package com.example.baytro.viewModel.contract

import com.example.baytro.data.contract.Status
import com.example.baytro.data.qr_session.PendingQrSession
import com.example.baytro.data.user.User
import com.example.baytro.utils.Utils

data class ContractDetailsFormState(
    val contractNumber: String = "",
    val buildingName: String = "N/A",
    val roomNumber: String = "N/A",
    val startDate: String = "",
    val endDate: String = "",
    val rentalFee: String = "0",
    val deposit: String = "0",
    val status: Status = Status.PENDING,
    val tenantList: List<User> = emptyList()
) {
    // --- Các thuộc tính tiện ích ---
    val tenantCount: Int
        get() = tenantList.size

    val hasActiveTenants: Boolean
        get() = tenantList.isNotEmpty()

    val isPendingContract: Boolean
        get() = status == Status.PENDING

    val isActiveContract: Boolean
        get() = status == Status.ACTIVE

    val formattedRentalFee: String
        get() = Utils.formatCurrency(rentalFee)

    val formattedDeposit: String
        get() = Utils.formatCurrency(deposit)

    // --- HÀM LOGIC ĐÃ ĐƯỢC CHUYỂN VÀO ĐÂY ---
    fun shouldShowAddFirstTenantPrompt(
        pendingSessions: List<PendingQrSession>,
        confirmingIds: Set<String>,
        decliningIds: Set<String>,
        isLandlord: Boolean
    ): Boolean {
        return isPendingContract &&
                !hasActiveTenants &&
                pendingSessions.isEmpty() &&
                confirmingIds.isEmpty() &&
                decliningIds.isEmpty() &&
                isLandlord
    }
}