package com.example.baytro.viewModel.dashboard

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.utils.SingleEvent
import com.example.baytro.utils.cloudFunctions.DashboardCloudFunctions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class UiRevenueDataPoint(
    val monthLabel: String,
    val revenue: Float
)

@Stable
data class LandlordDashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val username: String = "",
    val totalPendingActions: Int = 0,
    val pendingReadingsCount: Int = 0,
    val newJoinRequestsCount: Int = 0,
    val overdueBillsCount: Int = 0,
    val pendingRequestsCount: Int = 0,
    val monthlyRevenue: Double = 0.0,
    val unpaidBalance: Double = 0.0,
    val occupancyRate: Double = 0.0,
    val occupiedRooms: Int = 0,
    val totalRooms: Int = 0,
    val totalTenants: Int = 0,
    val totalBuildings: Int = 0,
    val activeContracts: Int = 0,
    val upcomingDeadlines: Int = 0,
    val recentPayments: Int = 0,
    val revenueHistory: List<UiRevenueDataPoint> = emptyList()
)

class LandlordDashboardVM(
    private val dashboardCloudFunctions: DashboardCloudFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandlordDashboardUiState())
    val uiState: StateFlow<LandlordDashboardUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchDashboardData()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            fetchDashboardData()
        }
    }

    private suspend fun fetchDashboardData() {
        val result = dashboardCloudFunctions.getLandlordDashboardData()

        result.onSuccess { data ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    username = data.username,
                    totalPendingActions = data.totalPendingActions,
                    pendingReadingsCount = data.pendingReadingsCount,
                    newJoinRequestsCount = data.newJoinRequestsCount,
                    overdueBillsCount = data.overdueBillsCount,
                    pendingRequestsCount = data.pendingRequestsCount,
                    monthlyRevenue = data.totalRevenueThisMonth,
                    unpaidBalance = data.totalUnpaidAmount,
                    occupancyRate = data.totalOccupancyRate / 100.0,
                    occupiedRooms = data.occupiedRoomCount,
                    totalRooms = data.totalRoomCount,
                    totalTenants = data.totalTenantsCount,
                    totalBuildings = data.totalBuildingsCount,
                    activeContracts = data.activeContractsCount,
                    upcomingDeadlines = data.upcomingBillingDeadlinesCount,
                    recentPayments = data.recentPaymentsCount,
                    revenueHistory = data.monthlyRevenueHistory.map { historyPoint ->
                        UiRevenueDataPoint(
                            monthLabel = historyPoint.month,
                            revenue = historyPoint.revenue.toFloat()
                        )
                    }
                )
            }
        }
        result.onFailure { e ->
            _errorEvent.emit(SingleEvent("Failed to load dashboard: ${e.message}"))
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    fun refreshDashboardData() {
        refresh()
    }
}