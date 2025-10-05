package com.example.baytro.viewModel.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.div
import kotlin.rem
import kotlin.text.toInt

data class TenantDashboardUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val contract: Contract? = null,
    val monthsStayed: Int = 0,
    val daysStayed: Int = 0,
    val billPaymentDeadline: Int = 0,
    val error: String? = null
)

class TenantDashboardVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val contractRepository: ContractRepository,
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantDashboardUiState())
    val uiState: StateFlow<TenantDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val user = userRepository.getById(currentUser.uid)
                    val contracts = contractRepository.getAll()
                    val activeContract = contracts.firstOrNull { contract ->
                        contract.tenantIds.contains(currentUser.uid) &&
                        contract.status == Status.ACTIVE
                    }

                    val (months, days) = if (activeContract != null) {
                        calculateStayDuration(activeContract.startDate)
                    } else {
                        Pair(0, 0)
                    }

                    val billPaymentDeadline = if (activeContract != null) {
                        val room = roomRepository.getById(activeContract.roomId)
                        val building = if (room != null) {
                            buildingRepository.getById(room.buildingId)
                        } else null
                        building?.paymentDue ?: 0
                    } else {
                        0
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        contract = activeContract,
                        monthsStayed = months,
                        daysStayed = days,
                        billPaymentDeadline = billPaymentDeadline
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }

    private fun calculateStayDuration(startDateString: String): Pair<Int, Int> {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val startDate = LocalDate.parse(startDateString, formatter)
            Log.d("TenantDashboardVM", "Start date: $startDate")
            val today = LocalDate.now()
            val totalDays = ChronoUnit.DAYS.between(startDate, today)
            val months = (totalDays / 30).toInt()
            val days = (totalDays % 30).toInt()
            Pair(months, days)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    fun getContractId(): String? = _uiState.value.contract?.id
}
