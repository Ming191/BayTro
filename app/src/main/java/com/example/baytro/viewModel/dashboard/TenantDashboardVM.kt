package com.example.baytro.viewModel.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import com.example.baytro.data.offline.OfflineContractRepository
import com.example.baytro.utils.SingleEvent
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
    val monthsStayed: Int = 0,
    val daysStayed: Int = 0,
    val billPaymentDeadline: Int = 0,
)

sealed interface TenantDashboardAction {
    object Refresh : TenantDashboardAction
}

sealed interface TenantDashboardEvent {
    object NavigateToEmptyContract : TenantDashboardEvent
}

class TenantDashboardVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val offlineContractRepository: OfflineContractRepository,
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository
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

    fun onAction(action: TenantDashboardAction) {
        when (action) {
            is TenantDashboardAction.Refresh -> {
                Log.d("TenantDashboardVM", "Refresh action received")
                loadDashboardData()
            }
        }
    }

    private fun loadDashboardData() {
        Log.d("TenantDashboardVM", "loadDashboardData() started - OFFLINE-FIRST")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _errorEvent.emit(SingleEvent("User not authenticated"))
                    _uiState.update { it.copy(isLoading = false) }
                    Log.e("TenantDashboardVM", "User not authenticated")
                    return@launch
                }

                val user = userRepository.getById(currentUser.uid)

                // OFFLINE-FIRST: Get active contract from cache
                val activeContract = offlineContractRepository.getActiveContractByUserId(currentUser.uid)

                if (activeContract == null) {
                    _event.emit(TenantDashboardEvent.NavigateToEmptyContract)
                    _uiState.update { it.copy(isLoading = false, user = user) }
                    return@launch
                }

                val (months, days) = calculateStayDuration(activeContract.startDate)
                val billPaymentDeadline = getBillPaymentDeadline(activeContract)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        contract = activeContract,
                        monthsStayed = months,
                        daysStayed = days,
                        billPaymentDeadline = billPaymentDeadline
                    )
                }

                Log.d("TenantDashboardVM", "Dashboard loaded from offline cache successfully")
            } catch (e: Exception) {
                Log.e("TenantDashboardVM", "Error loading dashboard data", e)
                _errorEvent.emit(SingleEvent("Failed to load dashboard: ${e.message}"))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun getBillPaymentDeadline(contract: Contract): Int {
        val room = roomRepository.getById(contract.roomId)
        val building = room?.let { buildingRepository.getById(it.buildingId) }
        return building?.paymentDue ?: 0
    }

    private fun calculateStayDuration(startDateString: String): Pair<Int, Int> {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val startDate = LocalDate.parse(startDateString, formatter)
            val today = LocalDate.now()
            val totalDays = ChronoUnit.DAYS.between(startDate, today)
            val months = (totalDays / 30).toInt()
            val days = (totalDays % 30).toInt()
            Pair(months, days)
        } catch (e: Exception) {
            Log.e("TenantDashboardVM", "Error calculating stay duration", e)
            Pair(0, 0)
        }
    }
}
