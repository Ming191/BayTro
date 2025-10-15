package com.example.baytro.viewModel.meter_reading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.service.MeterReadingCloudFunctions
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingMeterReadingsUiState(
    val pendingReadings: List<MeterReading> = emptyList(),
    val isLoading: Boolean = true,
    val processingReadingIds: Set<String> = emptySet()
)

sealed interface PendingMeterReadingsAction {
    data class ApproveReading(val readingId: String) : PendingMeterReadingsAction
    data class DeclineReading(val readingId: String, val reason: String) : PendingMeterReadingsAction
}

class PendingMeterReadingsVM(
    private val meterReadingRepository: MeterReadingRepository,
    private val meterReadingCloudFunctions: MeterReadingCloudFunctions,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingMeterReadingsUiState())
    val uiState: StateFlow<PendingMeterReadingsUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    init {
        loadPendingReadings()
    }

    fun onAction(action: PendingMeterReadingsAction) {
        when (action) {
            is PendingMeterReadingsAction.ApproveReading -> approveReading(action.readingId)
            is PendingMeterReadingsAction.DeclineReading -> declineReading(action.readingId, action.reason)
        }
    }

    private fun loadPendingReadings() {
        val landlordId = auth.currentUser?.uid
        if (landlordId == null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = false) }
                _errorEvent.emit(SingleEvent("User not authenticated"))
            }
            return
        }

        viewModelScope.launch {
            meterReadingRepository.listenForPendingReadings(landlordId)
                .catch { e ->
                    Log.e("PendingMeterReadingsVM", "Error loading pending readings", e)
                    _uiState.update { it.copy(isLoading = false) }
                    _errorEvent.emit(SingleEvent(e.message ?: "Failed to load readings"))
                }
                .collect { readings ->
                    _uiState.update {
                        it.copy(
                            pendingReadings = readings,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun approveReading(readingId: String) {
        if (_uiState.value.processingReadingIds.contains(readingId)) {
            Log.d("PendingMeterReadingsVM", "Reading $readingId is already being processed.")
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                processingReadingIds = currentState.processingReadingIds + readingId,
                pendingReadings = currentState.pendingReadings.filter { it.id != readingId }
            )
        }

        viewModelScope.launch {
            val result = meterReadingCloudFunctions.approveMeterReading(readingId)

            result.onFailure { e ->
                Log.e("PendingMeterReadingsVM", "Failed to approve reading", e)
                viewModelScope.launch {
                    _errorEvent.emit(SingleEvent(e.message ?: "Failed to approve reading"))
                }
                // Note: Don't add it back to the list; the Firestore listener will handle restoring it on failure.
            }

            // Always remove from processing set, regardless of outcome.
            _uiState.update { it.copy(processingReadingIds = it.processingReadingIds - readingId) }
        }
    }

    private fun declineReading(readingId: String, reason: String) {
        if (reason.isBlank()) {
            viewModelScope.launch {
                _errorEvent.emit(SingleEvent("Please provide a reason for declining"))
            }
            return
        }

        if (_uiState.value.processingReadingIds.contains(readingId)) {
            Log.d("PendingMeterReadingsVM", "Reading $readingId is already being processed.")
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                processingReadingIds = currentState.processingReadingIds + readingId,
                pendingReadings = currentState.pendingReadings.filter { it.id != readingId }
            )
        }

        viewModelScope.launch {
            val result = meterReadingCloudFunctions.declineMeterReading(readingId, reason)

            result.onFailure { e ->
                Log.e("PendingMeterReadingsVM", "Failed to decline reading", e)
                viewModelScope.launch {
                    _errorEvent.emit(SingleEvent(e.message ?: "Failed to decline reading"))
                }
            }
            
            _uiState.update { it.copy(processingReadingIds = it.processingReadingIds - readingId) }
        }
    }
}
