package com.example.baytro.viewModel.meter_reading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.utils.SingleEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 10L

data class MeterReadingHistoryUiState(
    val groupedReadings: List<MeterReadingGroup> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingNextPage: Boolean = false,
    val allReadingsLoaded: Boolean = false
)

data class MeterReadingGroup(
    val timestamp: Long,
    val electricityReading: MeterReading?,
    val waterReading: MeterReading?
)

sealed interface MeterReadingHistoryAction {
    data class LoadReadings(val contractId: String) : MeterReadingHistoryAction
    object LoadNextPage : MeterReadingHistoryAction
}

class MeterReadingHistoryVM(
    private val meterReadingRepository: MeterReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeterReadingHistoryUiState())
    val uiState: StateFlow<MeterReadingHistoryUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    private var contractId: String? = null
    private var lastReadingTimestamp: Long? = null

    fun onAction(action: MeterReadingHistoryAction) {
        when (action) {
            is MeterReadingHistoryAction.LoadReadings -> loadInitialReadings(action.contractId)
            is MeterReadingHistoryAction.LoadNextPage -> loadNextPage()
        }
    }

    private fun loadInitialReadings(contractId: String) {
        if (this.contractId == contractId) return
        this.contractId = contractId
        _uiState.update { MeterReadingHistoryUiState() } // Reset state for new contract
        lastReadingTimestamp = null
        loadReadingsPage(isInitialLoad = true)
    }
    
    private fun loadNextPage() {
        if (uiState.value.isLoadingNextPage || uiState.value.allReadingsLoaded) return
        loadReadingsPage(isInitialLoad = false)
    }

    private fun loadReadingsPage(isInitialLoad: Boolean) {
        contractId?.let { id ->
            viewModelScope.launch {
                _uiState.update {
                    if (isInitialLoad) it.copy(isLoading = true) else it.copy(isLoadingNextPage = true)
                }

                try {
                    // Assume repository supports pagination
                    val newReadings = meterReadingRepository.getReadingsByContractPaginated(
                        contractId = id,
                        pageSize = PAGE_SIZE,
                        startAfterTimestamp = lastReadingTimestamp
                    )

                    lastReadingTimestamp = newReadings.minOfOrNull { it.createdAt }
                    
                    val allLoaded = newReadings.size < PAGE_SIZE

                    val newGroups = groupReadingsBySubmission(newReadings)

                    _uiState.update {
                        it.copy(
                            groupedReadings = if (isInitialLoad) newGroups else it.groupedReadings + newGroups,
                            isLoading = false,
                            isLoadingNextPage = false,
                            allReadingsLoaded = allLoaded
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MeterReadingHistoryVM", "Error loading readings", e)
                    _uiState.update { it.copy(isLoading = false, isLoadingNextPage = false) }
                    _errorEvent.emit(SingleEvent(e.message ?: "Failed to load readings"))
                }
            }
        }
    }

    private fun groupReadingsBySubmission(readings: List<MeterReading>): List<MeterReadingGroup> {
        val timeWindowMs = 5 * 60 * 1000L // 5 minutes
        val groups = mutableListOf<MeterReadingGroup>()
        val processedIds = mutableSetOf<String>()

        readings.sortedByDescending { it.createdAt }.forEach { reading ->
            if (reading.id in processedIds) return@forEach
            processedIds.add(reading.id)

            val matchingReading = readings.find { other ->
                other.id !in processedIds &&
                        other.type != reading.type &&
                        kotlin.math.abs(other.createdAt - reading.createdAt) < timeWindowMs
            }
            matchingReading?.let { processedIds.add(it.id) }

            val group = if (reading.type.name == "ELECTRICITY") {
                MeterReadingGroup(reading.createdAt, reading, matchingReading)
            } else {
                MeterReadingGroup(reading.createdAt, matchingReading, reading)
            }
            groups.add(group)
        }
        return groups.sortedByDescending { it.timestamp }
    }
}
