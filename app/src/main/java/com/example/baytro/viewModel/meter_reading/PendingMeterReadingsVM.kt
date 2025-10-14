package com.example.baytro.viewModel.meter_reading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.service.MeterReadingCloudFunctions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingMeterReadingsUiState(
    val pendingReadings: List<MeterReading> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val processingReadingIds: Set<String> = emptySet()
)

class PendingMeterReadingsVM(
    private val meterReadingRepository: MeterReadingRepository,
    private val meterReadingCloudFunctions: MeterReadingCloudFunctions,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingMeterReadingsUiState())
    val uiState: StateFlow<PendingMeterReadingsUiState> = _uiState.asStateFlow()

    init {
        loadPendingReadings()
    }

    private fun loadPendingReadings() {
        val landlordId = auth.currentUser?.uid
        if (landlordId == null) {
            _uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
            return
        }

        viewModelScope.launch {
            meterReadingRepository.listenForPendingReadings(landlordId)
                .catch { e ->
                    Log.e("PendingMeterReadingsVM", "Error loading pending readings", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { readings ->
                    _uiState.update {
                        it.copy(
                            pendingReadings = readings,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun approveReading(readingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(processingReadingIds = it.processingReadingIds + readingId) }

            val result = meterReadingCloudFunctions.approveMeterReading(readingId)

            result.onSuccess { message ->
                Log.d("PendingMeterReadingsVM", "Approved reading: $message")
                // The reading will be automatically removed from pending list via Firestore listener
            }.onFailure { e ->
                Log.e("PendingMeterReadingsVM", "Failed to approve reading", e)
                _uiState.update { it.copy(error = e.message) }
            }

            _uiState.update { it.copy(processingReadingIds = it.processingReadingIds - readingId) }
        }
    }

    fun declineReading(readingId: String, reason: String) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(error = "Please provide a reason for declining") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(processingReadingIds = it.processingReadingIds + readingId) }

            val result = meterReadingCloudFunctions.declineMeterReading(readingId, reason)

            result.onSuccess { message ->
                Log.d("PendingMeterReadingsVM", "Declined reading: $message")
                // The reading will be automatically removed from pending list via Firestore listener
            }.onFailure { e ->
                Log.e("PendingMeterReadingsVM", "Failed to decline reading", e)
                _uiState.update { it.copy(error = e.message) }
            }

            _uiState.update { it.copy(processingReadingIds = it.processingReadingIds - readingId) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

