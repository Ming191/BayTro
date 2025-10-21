package com.example.baytro.viewModel.meter_reading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// IMPORTANT: Make sure this import points to your updated MeterReading data class
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.utils.SingleEvent
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 10L

data class MeterReadingHistoryUiState(
    val readings: List<MeterReading> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingNextPage: Boolean = false,
    val allReadingsLoaded: Boolean = false
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
    private var lastReadingTimestamp: Timestamp? = null

    fun onAction(action: MeterReadingHistoryAction) {
        when (action) {
            is MeterReadingHistoryAction.LoadReadings -> loadInitialReadings(action.contractId)
            is MeterReadingHistoryAction.LoadNextPage -> loadNextPage()
        }
    }

    private fun loadInitialReadings(contractId: String) {
        if (this.contractId == contractId && uiState.value.readings.isNotEmpty()) return
        this.contractId = contractId
        _uiState.update { MeterReadingHistoryUiState() }
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
                    val newReadings = meterReadingRepository.getReadingsByContractPaginated(
                        contractId = id,
                        pageSize = PAGE_SIZE,
                        startAfterTimestamp = lastReadingTimestamp
                    )
                    lastReadingTimestamp = newReadings.lastOrNull()?.createdAt
                    val allLoaded = newReadings.size < PAGE_SIZE
                    _uiState.update { currentState ->
                        currentState.copy(
                            readings = if (isInitialLoad) newReadings else currentState.readings + newReadings,
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
}