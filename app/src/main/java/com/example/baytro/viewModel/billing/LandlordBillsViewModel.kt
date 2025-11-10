package com.example.baytro.viewModel.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.BuildingSummary
import com.example.baytro.data.billing.BillRepository
import com.example.baytro.data.billing.BillStatus
import com.example.baytro.data.billing.BillSummary
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class LandlordBillsUiState(
    val isLoading: Boolean = true,
    val buildings: List<BuildingSummary> = emptyList(),
    val selectedBuildingId: String? = null,
    val selectedDate: Pair<Int, Int>,
    val selectedStatus: BillStatus? = null,
    val pendingReadingsCount: Int = 0,
    val totalPendingReadings: Int = 0,
    val filteredBills: List<BillSummary> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class LandlordBillsViewModel(
    private val billRepository: BillRepository,
    private val meterReadingRepository: MeterReadingRepository,
    private val buildingRepository: BuildingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _buildings = MutableStateFlow<List<BuildingSummary>>(emptyList())
    private val _selectedBuildingId = MutableStateFlow<String?>(null)
    private val _selectedDate: MutableStateFlow<Pair<Int, Int>>
    private val _selectedStatus = MutableStateFlow<BillStatus?>(null)
    private val _pendingCountsByBuilding = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _isInitialized = MutableStateFlow(false)

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    val uiState: StateFlow<LandlordBillsUiState>

    init {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        _selectedDate = MutableStateFlow(Pair(currentMonth, currentYear))

        val landlordId = auth.currentUser?.uid


        val dynamicDataFlow = combine(
            _selectedBuildingId,
            _selectedDate
        ) { buildingId, date ->
            Pair(buildingId, date)
        }.distinctUntilChanged().flatMapLatest { (buildingId, date) ->
            _isLoading.value = true
            if (landlordId == null || buildingId == null) {
                _isLoading.value = false
                flowOf(Pair(emptyList(), 0))
            } else {
                combine(
                    billRepository.listenForBillsByBuildingAndMonth(landlordId, buildingId, date.first, date.second, _buildings.value)
                        .catch { e -> _errorEvent.emit(SingleEvent("Failed to load bills: ${e.message}")); emit(emptyList()) },
                    meterReadingRepository.listenForPendingReadingsByBuilding(landlordId, buildingId)
                        .map { it.size }
                        .catch { e -> _errorEvent.emit(SingleEvent("Failed to load pending readings: ${e.message}")); emit(0) }
                ) { bills, pendingCount ->
                    _isLoading.value = false
                    Pair(bills, pendingCount)
                }
            }
        }

        uiState = combine(
            _isLoading, _buildings, _selectedBuildingId, _selectedDate, _selectedStatus, _pendingCountsByBuilding, dynamicDataFlow
        ) { values ->
            val isLoading = values[0] as Boolean
            val buildings = values[1] as List<BuildingSummary>
            val selectedBuilding = values[2] as String?
            val selectedDate = values[3] as Pair<Int, Int>
            val selectedStatus = values[4] as BillStatus?
            val pendingCountsMap = values[5] as Map<String, Int>
            val (allBills, selectedPendingCount) = values[6] as Pair<List<BillSummary>, Int>

            val filteredBills = if (selectedStatus == null) allBills else allBills.filter { it.status == selectedStatus }

            val buildingsWithCounts = buildings.map { it.copy(pendingCount = pendingCountsMap[it.id] ?: 0) }

            LandlordBillsUiState(
                isLoading = isLoading,
                buildings = buildingsWithCounts,
                selectedBuildingId = selectedBuilding,
                selectedDate = selectedDate,
                selectedStatus = selectedStatus,
                pendingReadingsCount = selectedPendingCount,
                totalPendingReadings = pendingCountsMap.values.sum(),
                filteredBills = filteredBills
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LandlordBillsUiState(selectedDate = Pair(currentMonth, currentYear))
        )
    }

    fun initialize() {
        val landlordId = auth.currentUser?.uid
        if (landlordId == null) {
            viewModelScope.launch { _errorEvent.emit(SingleEvent("User not authenticated.")) }
            _isLoading.value = false
            return
        }

        if (_isInitialized.value) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val buildings = buildingRepository.getBuildingSummariesByLandlord(landlordId)
                _buildings.value = buildings

                if (_selectedBuildingId.value == null) {
                    _selectedBuildingId.value = buildings.firstOrNull()?.id
                }
                listenForAllPendingCounts(landlordId, buildings.map { it.id })
                _isInitialized.value = true
            } catch (e: Exception) {
                _errorEvent.emit(SingleEvent("Failed to load buildings: ${e.message}"))
                _isLoading.value = false
            }
        }
    }

    private fun listenForAllPendingCounts(landlordId: String, buildingIds: List<String>) {
        if (buildingIds.isEmpty()) {
            _isLoading.value = false
            return
        }

        val countFlows = buildingIds.map { buildingId ->
            meterReadingRepository.listenForPendingReadingsByBuilding(landlordId, buildingId)
                .map { buildingId to it.size }
                .catch { emit(buildingId to 0) }
        }

        combine(countFlows) { countsArray ->
            _pendingCountsByBuilding.value = countsArray.toMap()
        }.launchIn(viewModelScope)
    }

    fun selectBuilding(buildingId: String) {
        if (uiState.value.selectedBuildingId == buildingId) return
        _selectedBuildingId.value = buildingId
    }

    fun selectStatus(status: BillStatus?) {
        _selectedStatus.value = status
    }

    private fun updateMonth(offset: Int) {
        val (month, year) = _selectedDate.value
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.YEAR, year)
            add(Calendar.MONTH, offset)
        }
        _selectedDate.value = Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }

    fun goToNextMonth() = updateMonth(1)
    fun goToPreviousMonth() = updateMonth(-1)
}