package com.example.baytro.viewModel.meter_reading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// IMPORTANT: Make sure this import points to your updated MeterReading data class
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
    val processingReadingIds: Set<String> = emptySet(),
    val dismissingReadingIds: Set<String> = emptySet()
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
        if (readingId in _uiState.value.processingReadingIds) return

        _uiState.update { it.copy(processingReadingIds = it.processingReadingIds + readingId) }

        viewModelScope.launch {
            try {
                val result = meterReadingCloudFunctions.approveMeterReading(readingId)

                result.onSuccess {
                    Log.d("PendingMeterReadingsVM", "Successfully approved reading $readingId")
                    // Trigger dismissal animation for this specific item
                    _uiState.update { it.copy(dismissingReadingIds = it.dismissingReadingIds + readingId) }
                    kotlinx.coroutines.delay(500) // Wait for animation
                }

                result.onFailure { e ->
                    Log.e("PendingMeterReadingsVM", "Failed to approve reading $readingId", e)
                    _errorEvent.emit(SingleEvent(e.message ?: "Failed to approve reading"))
                }

            } catch (e: Exception) {
                Log.e("PendingMeterReadingsVM", "An unexpected error occurred while approving reading $readingId", e)
                _errorEvent.emit(SingleEvent(e.message ?: "An unknown error occurred"))
            } finally {
                _uiState.update { it.copy(processingReadingIds = it.processingReadingIds - readingId) }
            }
        }
    }

    private fun declineReading(readingId: String, reason: String) {
        if (readingId in _uiState.value.processingReadingIds) return

        _uiState.update { it.copy(processingReadingIds = it.processingReadingIds + readingId) }

        viewModelScope.launch {
            try {
                val result = meterReadingCloudFunctions.declineMeterReading(readingId, reason)

                result.onSuccess {
                    Log.d("PendingMeterReadingsVM", "Successfully declined reading $readingId")
                    _uiState.update { it.copy(dismissingReadingIds = it.dismissingReadingIds + readingId) }
                    kotlinx.coroutines.delay(500)
                }

                result.onFailure { e ->
                    Log.e("PendingMeterReadingsVM", "Failed to decline reading $readingId", e)
                    _errorEvent.emit(SingleEvent(e.message ?: "Failed to decline reading"))
                }

            } catch (e: Exception) {
                Log.e("PendingMeterReadingsVM", "An unexpected error occurred while declining reading $readingId", e)
                _errorEvent.emit(SingleEvent(e.message ?: "An unknown error occurred"))
            } finally {
                _uiState.update { it.copy(processingReadingIds = it.processingReadingIds - readingId) }
            }
        }
    }
}