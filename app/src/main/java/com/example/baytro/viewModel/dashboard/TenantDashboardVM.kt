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
        Log.d("TenantDashboardVM", "ViewModel initialized")
        loadDashboardData()
    }

    fun refresh() {
        Log.d("TenantDashboardVM", "refresh() called")
        loadDashboardData()
    }

    private fun loadDashboardData() {
        Log.d("TenantDashboardVM", "loadDashboardData() started")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            Log.d("TenantDashboardVM", "Set isLoading = true")
            try {
                val currentUser = authRepository.getCurrentUser()
                Log.d("TenantDashboardVM", "Current user: ${currentUser?.uid}")

                if (currentUser != null) {
                    val user = userRepository.getById(currentUser.uid)
                    Log.d("TenantDashboardVM", "User fetched: ${user?.fullName}")

                    val contracts = contractRepository.getAll()
                    Log.d("TenantDashboardVM", "Total contracts fetched: ${contracts.size}")

                    // Debug each contract
                    contracts.forEachIndexed { index, contract ->
                        Log.d("TenantDashboardVM", "Contract $index: id=${contract.id}, status=${contract.status}, tenantIds=${contract.tenantIds}, contains user=${contract.tenantIds.contains(currentUser.uid)}")
                    }

                    val activeContract = contracts.firstOrNull { contract ->
                        val containsUser = contract.tenantIds.contains(currentUser.uid)
                        val isActive = contract.status == Status.ACTIVE
                        Log.d("TenantDashboardVM", "Checking contract ${contract.id}: containsUser=$containsUser, isActive=$isActive")
                        containsUser && isActive
                    }
                    Log.d("TenantDashboardVM", "Active contract found: ${activeContract?.id}")

                    val (months, days) = if (activeContract != null) {
                        calculateStayDuration(activeContract.startDate)
                    } else {
                        Pair(0, 0)
                    }
                    Log.d("TenantDashboardVM", "Duration calculated: $months months, $days days")

                    val billPaymentDeadline = if (activeContract != null) {
                        val room = roomRepository.getById(activeContract.roomId)
                        Log.d("TenantDashboardVM", "Room fetched: ${room?.roomNumber}")
                        val building = if (room != null) {
                            buildingRepository.getById(room.buildingId)
                        } else null
                        Log.d("TenantDashboardVM", "Building fetched: ${building?.name}")
                        building?.paymentDue ?: 0
                    } else {
                        0
                    }
                    Log.d("TenantDashboardVM", "Bill payment deadline: $billPaymentDeadline")

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        contract = activeContract,
                        monthsStayed = months,
                        daysStayed = days,
                        billPaymentDeadline = billPaymentDeadline,
                        error = if (activeContract == null) "No active contract found" else null
                    )
                    Log.d("TenantDashboardVM", "Set isLoading = false, data loaded successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    Log.e("TenantDashboardVM", "User not authenticated")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
                Log.e("TenantDashboardVM", "Error loading dashboard data", e)
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
