package com.example.baytro.viewModel.meter_reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.BuildingSummary
import com.example.baytro.data.MeterStatus
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.service.MeterReadingCloudFunctions
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingMeterReadingsUiState(
    val isLoading: Boolean = true,
    val buildings: List<BuildingSummary> = emptyList(),
    val selectedBuildingId: String? = null,
    val readings: List<MeterReading> = emptyList(),
    val processingReadingIds: Set<String> = emptySet(),
    val dismissingReadingIds: Set<String> = emptySet()
)

sealed interface PendingMeterReadingsAction {
    data class ApproveReading(val readingId: String) : PendingMeterReadingsAction
    data class DeclineReading(val readingId: String, val reason: String) : PendingMeterReadingsAction
    data class SelectBuilding(val buildingId: String?) : PendingMeterReadingsAction
}

@OptIn(ExperimentalCoroutinesApi::class)
class PendingMeterReadingsVM(
    private val meterReadingRepository: MeterReadingRepository,
    private val meterReadingCloudFunctions: MeterReadingCloudFunctions,
    private val buildingRepository: BuildingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    private val _buildings = MutableStateFlow<List<BuildingSummary>>(emptyList())
    private val _selectedBuildingId = MutableStateFlow<String?>("ALL")
    private val _processingReadingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _dismissingReadingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()
    val uiState: StateFlow<PendingMeterReadingsUiState>
    private val metadataFlow = combine(
        _isLoading, _buildings, _selectedBuildingId
    ) { isLoading, buildings, selectedBuildingId ->
        Triple(isLoading, buildings, selectedBuildingId)
    }

    init {
        val landlordId = auth.currentUser?.uid

        val readingsFlow: Flow<List<MeterReading>> = _selectedBuildingId
            .flatMapLatest { buildingId ->
                _isLoading.value = true
                if (landlordId == null) {
                    flowOf(emptyList())
                } else {
                    val flow: Flow<List<MeterReading>> = if (buildingId == null || buildingId == "ALL") {
                        meterReadingRepository.listenForReadingsByStatus(landlordId, MeterStatus.PENDING)
                    } else {
                        meterReadingRepository.listenForPendingReadingsByBuilding(landlordId, buildingId)
                    }
                    flow.onEach { _isLoading.value = false }
                        .catch { e ->
                            _errorEvent.emit(SingleEvent("Failed to load readings: ${e.message}"))
                            emit(emptyList())
                        }
                }
            }

        uiState = combine(
            metadataFlow,
            _processingReadingIds,
            _dismissingReadingIds,
            readingsFlow
        ) { metadata, processingIds, dismissingIds, readings ->
            val (isLoading, buildings, selectedBuildingId) = metadata

            PendingMeterReadingsUiState(
                isLoading = isLoading,
                buildings = buildings,
                selectedBuildingId = selectedBuildingId,
                readings = readings,
                processingReadingIds = processingIds,
                dismissingReadingIds = dismissingIds
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PendingMeterReadingsUiState()
        )
    }
    fun initialize() {
        val landlordId = auth.currentUser?.uid
        if (landlordId == null) { /* ... */ return }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val buildings = listOf(BuildingSummary(id = "ALL", name = "All Buildings")) +
                        buildingRepository.getBuildingSummariesByLandlord(landlordId)
                _buildings.value = buildings

                if (_selectedBuildingId.value == null) {
                    _selectedBuildingId.value = "ALL"
                }
            } catch (e: Exception) {
                _errorEvent.emit(SingleEvent("Failed to load buildings: ${e.message}"))
            }
        }
    }
    fun onAction(action: PendingMeterReadingsAction) {
        when (action) {
            is PendingMeterReadingsAction.ApproveReading -> approveReading(action.readingId)
            is PendingMeterReadingsAction.DeclineReading -> declineReading(action.readingId, action.reason)
            is PendingMeterReadingsAction.SelectBuilding -> selectBuilding(action.buildingId)
        }
    }
    private fun selectBuilding(buildingId: String?) {
        val newSelection = buildingId ?: "ALL"
        if (_selectedBuildingId.value == newSelection) return
        _selectedBuildingId.value = newSelection
    }
    private fun approveReading(readingId: String) {
        if (readingId in _processingReadingIds.value) return
        _processingReadingIds.update { it + readingId }
        viewModelScope.launch {
            try {
                val result = meterReadingCloudFunctions.approveMeterReading(readingId)
                result.onSuccess {
                    _errorEvent.emit(SingleEvent("Reading approved successfully!"))

                    _dismissingReadingIds.update { it + readingId }
                }
                result.onFailure { e -> _errorEvent.emit(SingleEvent(e.message ?: "Failed to approve")) }
            } catch (e: Exception) { _errorEvent.emit(SingleEvent(e.message ?: "Unknown error")) }
            finally {
                _processingReadingIds.update { it - readingId }
                _dismissingReadingIds.update { it - readingId }
            }
        }
    }
    private fun declineReading(readingId: String, reason: String) {
        if (readingId in _processingReadingIds.value) return
        _processingReadingIds.update { it + readingId }
        viewModelScope.launch {
            try {
                val result = meterReadingCloudFunctions.declineMeterReading(readingId, reason)
                result.onSuccess {
                    _errorEvent.emit(SingleEvent("Reading declined successfully."))
                    _dismissingReadingIds.update { it + readingId }
                }
                result.onFailure { e -> _errorEvent.emit(SingleEvent(e.message ?: "Failed to decline")) }
            } catch (e: Exception) { _errorEvent.emit(SingleEvent(e.message ?: "Unknown error")) }
            finally {
                _processingReadingIds.update { it - readingId }
                _dismissingReadingIds.update { it - readingId }
            }
        }
    }
}