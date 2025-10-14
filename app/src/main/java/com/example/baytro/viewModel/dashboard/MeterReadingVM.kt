package com.example.baytro.viewModel.dashboard

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.MeterStatus
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.data.meter_reading.MeterType as MeterTypeData
import com.example.baytro.service.MeterReadingApiService
import com.example.baytro.service.MeterReadingCloudFunctions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

data class MeterReadingUiState(
    val isProcessing: Boolean = false,
    val isSubmitting: Boolean = false,
    val electricityReading: String = "",
    val waterReading: String = "",
    val recognizedText: String = "",
    val error: String? = null,
    val capturedImage: Bitmap? = null,
    val selectedPhotos: List<Uri> = emptyList(),
    val meterType: MeterType = MeterType.ELECTRICITY,
    val detectionConfidence: Float = 0f,
    val contractId: String = "",
    val roomId: String = "",
    val landlordId: String = ""
)

enum class MeterType {
    ELECTRICITY,
    WATER
}

class MeterReadingVM(
    private val meterReadingApiService: MeterReadingApiService,
    private val meterReadingRepository: MeterReadingRepository,
    private val mediaRepository: MediaRepository,
    private val meterReadingCloudFunctions: MeterReadingCloudFunctions,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(MeterReadingUiState())
    val uiState: StateFlow<MeterReadingUiState> = _uiState.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    fun initialize(contractId: String, roomId: String, landlordId: String) {
        _uiState.update {
            it.copy(
                contractId = contractId,
                roomId = roomId,
                landlordId = landlordId
            )
        }
    }

    fun setSelectedPhotos(uris: List<Uri>) {
        _uiState.update { it.copy(selectedPhotos = uris) }
    }

    private fun extractMeterReading(
        detections: List<com.example.baytro.data.MeterDetection>
    ): String {
        val reading = detections.sortedBy { it.x }.joinToString("") { it.label }
        return reading
    }

    fun updateElectricityReading(reading: String) {
        val filtered = reading.filter { it.isDigit() }
        _uiState.update { it.copy(electricityReading = filtered) }
    }

    fun updateWaterReading(reading: String) {
        val filtered = reading.filter { it.isDigit() }
        _uiState.update { it.copy(waterReading = filtered) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun processImageFromUri(uri: Uri, meterType: MeterType, context: android.content.Context) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isProcessing = true,
                        error = null,
                        meterType = meterType
                    )
                }

                // Convert URI to Bitmap
                val bitmap = android.graphics.BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(uri)
                )

                if (bitmap == null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = "Failed to load image"
                        )
                    }
                    return@launch
                }

                val result = meterReadingApiService.predictMeterReading(bitmap)

                result.onSuccess { response ->
                    Log.d("MeterReadingVM", "API Response - Text: ${response.text}, Detections: ${response.detections.size}")
                    val meterReading = extractMeterReading(response.detections)
                    val avgConfidence = if (response.detections.isNotEmpty()) {
                        response.detections.map { it.conf }.average().toFloat()
                    } else {
                        0f
                    }
                    if (meterReading.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                recognizedText = response.text,
                                isProcessing = false,
                                error = "No meter reading found. Please try again or enter manually."
                            )
                        }
                        return@launch
                    }
                    _uiState.update { currentState ->
                        when (meterType) {
                            MeterType.ELECTRICITY -> currentState.copy(
                                electricityReading = meterReading,
                                recognizedText = response.text,
                                detectionConfidence = avgConfidence,
                                isProcessing = false,
                                error = null
                            )
                            MeterType.WATER -> currentState.copy(
                                waterReading = meterReading,
                                recognizedText = response.text,
                                detectionConfidence = avgConfidence,
                                isProcessing = false,
                                error = null
                            )
                        }
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = "Failed to read meter: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Failed to process image: ${e.message}"
                    )
                }
            }
        }
    }

    fun submitReadings(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val currentUserId = auth.currentUser?.uid

                if (currentUserId == null) {
                    _uiState.update { it.copy(error = "User not authenticated") }
                    return@launch
                }

                if (state.contractId.isEmpty() || state.roomId.isEmpty() || state.landlordId.isEmpty()) {
                    _uiState.update { it.copy(error = "Missing contract information") }
                    return@launch
                }

                val electricityReading = state.electricityReading
                val waterReading = state.waterReading

                if (electricityReading.isEmpty() && waterReading.isEmpty()) {
                    _uiState.update { it.copy(error = "Please provide at least one meter reading") }
                    return@launch
                }

                _uiState.update { it.copy(isSubmitting = true, error = null) }
                _uploadProgress.value = 0.1f

                // Wrap entire submission in NonCancellable to prevent interruption
                withContext(NonCancellable) {
                    val hasElectricity = electricityReading.isNotEmpty()
                    val hasWater = waterReading.isNotEmpty()
                    val totalSteps = (if (hasElectricity) 2 else 0) + (if (hasWater) 2 else 0)
                    var currentStep = 0

                    // Prepare data structures to track operations for rollback
                    val uploadedImageUrls = mutableListOf<String>()
                    val createdReadingIds = mutableListOf<String>()

                    try {
                        // Step 1: Upload electricity image if needed
                        var electricityImageUrl: String? = null
                        if (hasElectricity && state.selectedPhotos.getOrNull(0) != null) {
                            _uploadProgress.value = 0.2f + (currentStep.toFloat() / totalSteps * 0.6f)
                            electricityImageUrl = mediaRepository.uploadImageFromUri(
                                uri = state.selectedPhotos[0],
                                path = "meter_readings/${state.contractId}/${System.currentTimeMillis()}_electricity.jpg"
                            )
                            uploadedImageUrls.add(electricityImageUrl)
                            Log.d("MeterReadingVM", "Electricity image uploaded: $electricityImageUrl")
                            currentStep++
                        }

                        // Step 2: Upload water image if needed
                        var waterImageUrl: String? = null
                        if (hasWater && state.selectedPhotos.getOrNull(1) != null) {
                            _uploadProgress.value = 0.2f + (currentStep.toFloat() / totalSteps * 0.6f)
                            waterImageUrl = mediaRepository.uploadImageFromUri(
                                uri = state.selectedPhotos[1],
                                path = "meter_readings/${state.contractId}/${System.currentTimeMillis()}_water.jpg"
                            )
                            uploadedImageUrls.add(waterImageUrl)
                            Log.d("MeterReadingVM", "Water image uploaded: $waterImageUrl")
                            currentStep++
                        }

                        // Step 3: Create electricity reading if provided
                        if (hasElectricity) {
                            _uploadProgress.value = 0.2f + (currentStep.toFloat() / totalSteps * 0.6f)
                            val electricityReadingDoc = MeterReading(
                                contractId = state.contractId,
                                roomId = state.roomId,
                                landlordId = state.landlordId,
                                tenantId = currentUserId,
                                type = MeterTypeData.ELECTRICITY,
                                value = electricityReading.toInt(),
                                imageUrl = electricityImageUrl,
                                status = MeterStatus.METER_PENDING,
                                createdAt = System.currentTimeMillis()
                            )
                            val readingId = meterReadingRepository.add(electricityReadingDoc)
                            createdReadingIds.add(readingId)
                            Log.d("MeterReadingVM", "Created electricity reading: $readingId")
                            currentStep++
                        }

                        // Step 4: Create water reading if provided
                        if (hasWater) {
                            _uploadProgress.value = 0.2f + (currentStep.toFloat() / totalSteps * 0.6f)
                            val waterReadingDoc = MeterReading(
                                contractId = state.contractId,
                                roomId = state.roomId,
                                landlordId = state.landlordId,
                                tenantId = currentUserId,
                                type = MeterTypeData.WATER,
                                value = waterReading.toInt(),
                                imageUrl = waterImageUrl,
                                status = MeterStatus.METER_PENDING,
                                createdAt = System.currentTimeMillis()
                            )
                            val readingId = meterReadingRepository.add(waterReadingDoc)
                            createdReadingIds.add(readingId)
                            Log.d("MeterReadingVM", "Created water reading: $readingId")
                            currentStep++
                        }

                        _uploadProgress.value = 0.85f

                        // Step 5: Notify landlord for all readings
                        // Use try-catch for each notification to prevent one failure from stopping others
                        for (readingId in createdReadingIds) {
                            try {
                                val notifyResult = meterReadingCloudFunctions.notifyNewMeterReading(readingId)
                                notifyResult.onFailure { e ->
                                    Log.e("MeterReadingVM", "Failed to notify landlord for reading $readingId", e)
                                }
                            } catch (e: Exception) {
                                Log.e("MeterReadingVM", "Exception notifying landlord for reading $readingId", e)
                            }
                        }

                        _uploadProgress.value = 0.95f
                        _uiState.update { it.copy(isSubmitting = false) }
                        _uploadProgress.value = 1.0f
                        Log.d("MeterReadingVM", "Successfully submitted all readings atomically")
                        onSuccess()

                    } catch (e: Exception) {
                        // ATOMIC ROLLBACK: Delete all created readings and uploaded images
                        Log.e("MeterReadingVM", "Error during submission, rolling back...", e)

                        // Rollback: Delete created readings
                        for (readingId in createdReadingIds) {
                            try {
                                meterReadingRepository.delete(readingId)
                                Log.d("MeterReadingVM", "Rolled back reading: $readingId")
                            } catch (rollbackError: Exception) {
                                Log.e("MeterReadingVM", "Failed to rollback reading $readingId", rollbackError)
                            }
                        }

                        // Rollback: Delete uploaded images
                        for (imageUrl in uploadedImageUrls) {
                            try {
                                mediaRepository.deleteImage(imageUrl)
                                Log.d("MeterReadingVM", "Rolled back image: $imageUrl")
                            } catch (rollbackError: Exception) {
                                Log.e("MeterReadingVM", "Failed to rollback image $imageUrl", rollbackError)
                            }
                        }

                        _uploadProgress.value = 0f
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = "Failed to submit readings: ${e.message}. All changes have been rolled back."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MeterReadingVM", "Error submitting readings", e)
                _uploadProgress.value = 0f
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = "Failed to submit readings: ${e.message}"
                    )
                }
            }
        }
    }
}
