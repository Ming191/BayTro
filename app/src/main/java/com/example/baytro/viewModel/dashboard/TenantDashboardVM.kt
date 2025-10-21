package com.example.baytro.viewModel.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.billing.Bill
import com.example.baytro.data.billing.BillRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.SingleEvent
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val monthsStayed: Int = 0,
    val daysStayed: Int = 0
)

sealed interface TenantDashboardEvent {
    object NavigateToEmptyContract : TenantDashboardEvent
}

class TenantDashboardVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val contractRepository: ContractRepository,
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val meterReadingRepository: MeterReadingRepository,
    private val billRepository: BillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantDashboardUiState())
    val uiState: StateFlow<TenantDashboardUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    private val _event = MutableSharedFlow<TenantDashboardEvent>()
    val event: SharedFlow<TenantDashboardEvent> = _event.asSharedFlow()

    init {
        Log.d("TenantDashboardVM", "ViewModel initialized")
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: run {
                        _errorEvent.emit(SingleEvent("User not authenticated"))
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    }

                // Parallel user + contract
                val (user, activeContract) = coroutineScope {
                    val userDef = async {
                        try {
                            userRepository.getById(currentUser.uid)
                        } catch (e: Exception) {
                            Log.e("TenantDashboardVM", "User fetch failed", e)
                            _errorEvent.emit(SingleEvent("Profile unavailable"))
                            null
                        }
                    }
                    val contractDef = async {
                        try {
                            contractRepository.getActiveContract(currentUser.uid)
                        } catch (e: Exception) {
                            Log.e("TenantDashboardVM", "Contract fetch failed", e)
                            _errorEvent.emit(SingleEvent("Contract unavailable"))
                            null
                        }
                    }
                    userDef.await() to contractDef.await()
                }

                if (activeContract == null) {
                    _event.emit(TenantDashboardEvent.NavigateToEmptyContract)
                    _uiState.update { it.copy(isLoading = false, user = user) }
                    return@launch
                }

                val (room, building, lastReading, currentBill) = coroutineScope {
                    val roomDef = async {
                        try { roomRepository.getById(activeContract.roomId) } catch (e: Exception) { Log.e("TenantDashboardVM", "Room fetch failed", e); null }
                    }
                    val buildingDef = async {
                        try { buildingRepository.getById(activeContract.buildingId) } catch (e: Exception) { Log.e("TenantDashboardVM", "Building fetch failed", e); null }
                    }
                    val lastReadingDef = async {
                        try { meterReadingRepository.getLastApprovedReading(activeContract.id) } catch (e: Exception) { Log.e("TenantDashboardVM", "Reading fetch failed", e); null }
                    }
                    val currentBillDef = async {
                        try { billRepository.getCurrentBillByContract(activeContract.id) } catch (e: Exception) { Log.e("TenantDashboardVM", "Bill fetch failed", e); null }
                    }

                    Quadruple(
                        roomDef.await(),
                        buildingDef.await(),
                        lastReadingDef.await(),
                        currentBillDef.await()
                    )
                }

                val (months, days) = calculateStayDuration(activeContract.startDate)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        contract = activeContract,
                        room = room,
                        building = building,
                        lastApprovedReading = lastReading?.getOrNull(),
                        currentBill = currentBill?.getOrNull(),
                        monthsStayed = months,
                        daysStayed = days
                    )
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e is dev.gitlive.firebase.firestore.FirebaseFirestoreException &&
                            e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> {
                                "Offline—retrying when connected"
                            }
                    else -> "Dashboard load failed: ${e.message}"
                }
                _errorEvent.emit(SingleEvent(errorMsg))
                Log.e("TenantDashboardVM", "Error loading dashboard", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun calculateStayDuration(startDateString: String): Pair<Int, Int> {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val startDate = LocalDate.parse(startDateString, formatter)
            val today = LocalDate.now()

            val years = startDate.until(today, ChronoUnit.YEARS)
            val months = startDate.plusYears(years).until(today, ChronoUnit.MONTHS)
            val days = startDate.plusYears(years).plusMonths(months).until(today, ChronoUnit.DAYS)

            val totalMonths = years * 12 + months
            Pair(totalMonths.toInt(), days.toInt())
        } catch (e: Exception) {
            Log.e("TenantDashboardVM", "Error calculating stay duration", e)
            Pair(0, 0)
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)