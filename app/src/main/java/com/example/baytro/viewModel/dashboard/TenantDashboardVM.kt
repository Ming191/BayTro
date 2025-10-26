package com.example.baytro.viewModel.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.Building
import com.example.baytro.data.room.Room
import com.example.baytro.data.billing.Bill
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.service.Service
import com.example.baytro.data.user.User
import com.example.baytro.utils.SingleEvent
import com.example.baytro.utils.cloudFunctions.DashboardCloudFunctions
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class TenantDashboardUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val contract: Contract? = null,
    val room: Room? = null,
    val building: Building? = null,
    val lastApprovedReading: MeterReading? = null,
    val currentBill: Bill? = null,
    val fixedServices: List<Service> = emptyList(),
    val monthsStayed: Int = 0,
    val daysStayed: Int = 0
)

sealed interface TenantDashboardEvent {
    object NavigateToEmptyContract : TenantDashboardEvent
}

class TenantDashboardVM(
    private val dashboardCloudFunctions: DashboardCloudFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantDashboardUiState())
    val uiState: StateFlow<TenantDashboardUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    private val _event = MutableSharedFlow<TenantDashboardEvent>()
    val event: SharedFlow<TenantDashboardEvent> = _event.asSharedFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = dashboardCloudFunctions.getTenantDashboardData()

            result.onSuccess { data ->
                if (data.contract == null) {
                    _event.emit(TenantDashboardEvent.NavigateToEmptyContract)
                    _uiState.update { it.copy(isLoading = false, user = data.user) }
                } else {
                    val (months, days) = calculateStayDuration(data.contract.startDate)

                    Log.d("TenantDashboardVM", "Room extra services: ${data.room?.extraService}")
                    Log.d("TenantDashboardVM", "Room extra services size: ${data.room?.extraService?.size}")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = data.user,
                            contract = data.contract,
                            room = data.room,
                            building = data.building,
                            lastApprovedReading = data.lastApprovedReading,
                            currentBill = data.currentBill,
                            fixedServices = data.room?.extraService ?: emptyList(),
                            monthsStayed = months,
                            daysStayed = days
                        )
                    }
                }
            }
            result.onFailure { e ->
                _errorEvent.emit(SingleEvent("Failed to load dashboard: ${e.message}"))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun calculateStayDuration(startDateString: String): Pair<Int, Int> {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val startDate = LocalDate.parse(startDateString, formatter)
            val today = LocalDate.now()

            val totalMonths = ChronoUnit.MONTHS.between(startDate, today)
            val daysAfterMonths = startDate.plusMonths(totalMonths).until(today, ChronoUnit.DAYS)

            Pair(totalMonths.toInt(), daysAfterMonths.toInt())
        } catch (_: Exception) {
            Pair(0, 0)
        }
    }
}