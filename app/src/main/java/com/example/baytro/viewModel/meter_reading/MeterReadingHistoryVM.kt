package com.example.baytro.viewModel.meter_reading

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MeterReadingHistoryUiState(
    val readings: List<MeterReading> = emptyList(),
    val groupedReadings: List<MeterReadingGroup> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class MeterReadingGroup(
    val timestamp: Long,
    val electricityReading: MeterReading?,
    val waterReading: MeterReading?
)

class MeterReadingHistoryVM(
    private val meterReadingRepository: MeterReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeterReadingHistoryUiState())
    val uiState: StateFlow<MeterReadingHistoryUiState> = _uiState.asStateFlow()

    private var contractId: String? = null

    fun loadReadings(contractId: String) {
        if (this.contractId == contractId) return
        this.contractId = contractId

        viewModelScope.launch {
            meterReadingRepository.listenForReadingsByContract(contractId)
                .catch { e ->
                    Log.e("MeterReadingHistoryVM", "Error loading readings", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { readings ->
                    val grouped = groupReadingsBySubmission(readings)
                    _uiState.update {
                        it.copy(
                            readings = readings,
                            groupedReadings = grouped,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun groupReadingsBySubmission(readings: List<MeterReading>): List<MeterReadingGroup> {
        // Group readings that were submitted within 5 minutes of each other
        val timeWindowMs = 5 * 60 * 1000L // 5 minutes

        val groups = mutableListOf<MeterReadingGroup>()
        val processedIds = mutableSetOf<String>()

        readings.sortedByDescending { it.createdAt }.forEach { reading ->
            if (reading.id in processedIds) return@forEach

            processedIds.add(reading.id)

            // Find matching reading of opposite type within time window
            val matchingReading = readings.find { other ->
                other.id !in processedIds &&
                        other.type != reading.type &&
                        kotlin.math.abs(other.createdAt - reading.createdAt) < timeWindowMs
            }

            matchingReading?.let { processedIds.add(it.id) }

            val group = when (reading.type.name) {
                "ELECTRICITY" -> MeterReadingGroup(
                    timestamp = reading.createdAt,
                    electricityReading = reading,
                    waterReading = matchingReading
                )
                "WATER" -> MeterReadingGroup(
                    timestamp = reading.createdAt,
                    electricityReading = matchingReading,
                    waterReading = reading
                )
                else -> MeterReadingGroup(
                    timestamp = reading.createdAt,
                    electricityReading = if (reading.type.name == "ELECTRICITY") reading else null,
                    waterReading = if (reading.type.name == "WATER") reading else null
                )
            }

            groups.add(group)
        }

        return groups.sortedByDescending { it.timestamp }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
