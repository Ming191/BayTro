package com.example.baytro.viewModel.meter_reading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.service.MeterReadingCloudFunctions
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingMeterReadingsUiState(
    val pendingGroups: List<PendingReadingGroup> = emptyList(),
    val isLoading: Boolean = true,
    val processingReadingIds: Set<String> = emptySet(),
    val dismissingGroupTimestamps: Set<Long> = emptySet()
)

data class PendingReadingGroup(
    val timestamp: Long,
    val electricityReading: MeterReading?,
    val waterReading: MeterReading?
)

sealed interface PendingMeterReadingsAction {
    data class ApproveReading(val readingId: String) : PendingMeterReadingsAction
    data class DeclineReading(val readingId: String, val reason: String) : PendingMeterReadingsAction
    data class ApproveGroup(val electricityReadingId: String?, val waterReadingId: String?) : PendingMeterReadingsAction
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
            is PendingMeterReadingsAction.ApproveGroup -> approveGroup(action.electricityReadingId, action.waterReadingId)
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
                    val groups = groupReadingsBySubmission(readings)
                    _uiState.update {
                        it.copy(
                            pendingGroups = groups,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun groupReadingsBySubmission(readings: List<MeterReading>): List<PendingReadingGroup> {
        val timeWindowMs = 5 * 60 * 1000L // 5 minutes
        val groups = mutableListOf<PendingReadingGroup>()
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
                PendingReadingGroup(reading.createdAt, reading, matchingReading)
            } else {
                PendingReadingGroup(reading.createdAt, matchingReading, reading)
            }
            groups.add(group)
        }
        return groups.sortedByDescending { it.timestamp }
    }

    private fun approveGroup(electricityReadingId: String?, waterReadingId: String?) {
        val readingIds = listOfNotNull(electricityReadingId, waterReadingId)

        if (readingIds.isEmpty()) return

        if (readingIds.any { _uiState.value.processingReadingIds.contains(it) }) {
            Log.d("PendingMeterReadingsVM", "One or more readings are already being processed.")
            return
        }

        val groupTimestamp = _uiState.value.pendingGroups.find { group ->
            group.electricityReading?.id == electricityReadingId ||
                    group.waterReading?.id == waterReadingId
        }?.timestamp

        _uiState.update { currentState ->
            currentState.copy(
                processingReadingIds = currentState.processingReadingIds + readingIds.toSet()
            )
        }

        viewModelScope.launch {
            try {
                coroutineScope {
                    val deferredResults = readingIds.map { readingId ->
                        async {
                            val result = meterReadingCloudFunctions.approveMeterReading(readingId)
                            readingId to result
                        }
                    }

                    val results = awaitAll(*deferredResults.toTypedArray())

                    var hasFailure = false
                    results.forEach { (readingId, result) ->
                        result.onFailure { e ->
                            hasFailure = true
                            Log.e("PendingMeterReadingsVM", "Failed to approve reading $readingId", e)
                            _errorEvent.emit(SingleEvent(e.message ?: "Failed to approve reading"))
                        }
                    }

                    if (!hasFailure && groupTimestamp != null) {
                        _uiState.update {
                            it.copy(dismissingGroupTimestamps = it.dismissingGroupTimestamps + groupTimestamp)
                        }
                        kotlinx.coroutines.delay(500) // Animation duration + small buffer
                    }
                }

                _uiState.update { it.copy(processingReadingIds = it.processingReadingIds - readingIds.toSet()) }
            } catch (e: Exception) {
                Log.e("PendingMeterReadingsVM", "Error approving group", e)
                _errorEvent.emit(SingleEvent(e.message ?: "Failed to approve readings"))
                _uiState.update { it.copy(processingReadingIds = it.processingReadingIds - readingIds.toSet()) }
            }
        }
    }

    private fun approveReading(readingId: String) {
        Log.w("PendingMeterReadingsVM", "Single reading approval not supported")
    }

    private fun declineReading(readingId: String, reason: String) {
        Log.w("PendingMeterReadingsVM", "Single reading decline not supported")
    }
}
