package com.example.baytro.viewModel.dashboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.MeterStatus
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.data.meter_reading.MeterType
import com.example.baytro.service.MeterReadingApiService
import com.example.baytro.service.MeterReadingCloudFunctions
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

data class MeterReadingUiState(
    val isProcessing: Boolean = false,
    val isSubmitting: Boolean = false,
    val readings: Map<MeterType, String> = emptyMap(),
    val selectedPhotos: Map<MeterType, Uri> = emptyMap(),
    val recognizedText: String = "",
    val capturedImage: Bitmap? = null,
    val meterType: MeterType = MeterType.ELECTRICITY,
    val detectionConfidence: Float = 0f,
    val contractId: String = "",
    val roomId: String = "",
    val landlordId: String = ""
)

sealed interface MeterReadingEvent {
    object SubmissionSuccess : MeterReadingEvent
}

sealed interface MeterReadingAction {
    data class UpdateReading(val meterType: MeterType, val reading: String) : MeterReadingAction
    data class SelectPhoto(val meterType: MeterType, val uri: Uri) : MeterReadingAction
    data class ProcessImage(val meterType: MeterType, val uri: Uri, val context: Context) : MeterReadingAction
    object SubmitReadings : MeterReadingAction
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

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    private val _event = MutableSharedFlow<MeterReadingEvent>()
    val event: SharedFlow<MeterReadingEvent> = _event.asSharedFlow()

    fun onAction(action: MeterReadingAction) {
        when (action) {
            is MeterReadingAction.UpdateReading -> updateReading(action.meterType, action.reading)
            is MeterReadingAction.SelectPhoto -> setSelectedPhoto(action.meterType, action.uri)
            is MeterReadingAction.ProcessImage -> processImageFromUri(action.uri, action.meterType, action.context)
            is MeterReadingAction.SubmitReadings -> submitReadings()
        }
    }
    
    fun initialize(contractId: String, roomId: String, landlordId: String) {
        _uiState.update {
            it.copy(
                contractId = contractId,
                roomId = roomId,
                landlordId = landlordId
            )
        }
    }

    private fun setSelectedPhoto(meterType: MeterType, uri: Uri) {
        _uiState.update { currentState ->
            val newPhotos = currentState.selectedPhotos.toMutableMap()
            newPhotos[meterType] = uri
            currentState.copy(selectedPhotos = newPhotos)
        }
    }

    private fun extractMeterReading(
        detections: List<com.example.baytro.data.MeterDetection>
    ): String {
        return detections.sortedBy { it.x }.joinToString("") { it.label }
    }

    private fun updateReading(meterType: MeterType, reading: String) {
        val filtered = reading.filter { it.isDigit() }
        _uiState.update { currentState ->
            val newReadings = currentState.readings.toMutableMap()
            newReadings[meterType] = filtered
            currentState.copy(readings = newReadings)
        }
    }

    private fun processImageFromUri(uri: Uri, meterType: MeterType, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true, meterType = meterType) }

                // Perform heavy bitmap decoding in a background thread
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }

                if (bitmap == null) {
                    _uiState.update { it.copy(isProcessing = false) }
                    _errorEvent.emit(SingleEvent("Failed to load image"))
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
                            )
                        }
                        _errorEvent.emit(SingleEvent("No meter reading found. Please try again or enter manually."))
                        return@launch
                    }
                    _uiState.update { currentState ->
                        val newReadings = currentState.readings.toMutableMap()
                        newReadings[meterType] = meterReading
                        currentState.copy(
                            readings = newReadings,
                            recognizedText = response.text,
                            detectionConfidence = avgConfidence,
                            isProcessing = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false) }
                    _errorEvent.emit(SingleEvent("Failed to read meter: ${e.message}"))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false) }
                _errorEvent.emit(SingleEvent("Failed to process image: ${e.message}"))
            }
        }
    }

    private fun submitReadings() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _errorEvent.emit(SingleEvent("User not authenticated"))
                return@launch
            }

            if (state.contractId.isEmpty() || state.roomId.isEmpty() || state.landlordId.isEmpty()) {
                _errorEvent.emit(SingleEvent("Missing contract information"))
                return@launch
            }

            val readingsToSubmit = state.readings.filter { it.value.isNotEmpty() }
            if (readingsToSubmit.isEmpty()) {
                _errorEvent.emit(SingleEvent("Please provide at least one meter reading"))
                return@launch
            }

            _uiState.update { it.copy(isSubmitting = true) }
            _uploadProgress.value = 0.1f

            withContext(NonCancellable) {
                val uploadedImageUrls = mutableMapOf<MeterType, String>()
                val createdReadingIds = mutableListOf<String>()

                try {
                    // Refactored submission logic with clearer progress updates
                    val totalSteps = readingsToSubmit.count { state.selectedPhotos.containsKey(it.key) } + readingsToSubmit.size
                    var currentStep = 0

                    // Step 1: Upload images
                    for ((meterType, _) in readingsToSubmit) {
                        val photoUri = state.selectedPhotos[meterType]
                        if (photoUri != null) {
                            val imageUrl = uploadReadingImage(photoUri, state.contractId, meterType)
                            uploadedImageUrls[meterType] = imageUrl
                            currentStep++
                            _uploadProgress.value = 0.1f + (currentStep.toFloat() / totalSteps * 0.75f)
                        }
                    }

                    // Step 2: Create readings
                    for ((meterType, readingValue) in readingsToSubmit) {
                        val readingId = createMeterReading(
                            state,
                            currentUserId,
                            meterType,
                            readingValue,
                            uploadedImageUrls[meterType]
                        )
                        createdReadingIds.add(readingId)
                        currentStep++
                        _uploadProgress.value = 0.1f + (currentStep.toFloat() / totalSteps * 0.75f)
                    }

                    // Step 3: Notify landlord
                    notifyLandlord(createdReadingIds)
                    _uploadProgress.value = 0.95f

                    // Finalize
                    _uiState.update { it.copy(isSubmitting = false) }
                    _uploadProgress.value = 1.0f
                    Log.d("MeterReadingVM", "Successfully submitted all readings.")
                    _event.emit(MeterReadingEvent.SubmissionSuccess)

                } catch (e: Exception) {
                    Log.e("MeterReadingVM", "Error during submission, rolling back...", e)
                    rollbackSubmission(createdReadingIds, uploadedImageUrls.values.toList())
                    _uploadProgress.value = 0f
                    _uiState.update { it.copy(isSubmitting = false) }
                    _errorEvent.emit(SingleEvent("Submission failed: ${e.message}. All changes were rolled back."))
                }
            }
        }
    }

    private suspend fun uploadReadingImage(uri: Uri, contractId: String, meterType: MeterType): String {
        val path = "meter_readings/$contractId/${System.currentTimeMillis()}_${meterType.name.lowercase()}.jpg"
        val imageUrl = mediaRepository.uploadImageFromUri(uri = uri, path = path)
        Log.d("MeterReadingVM", "${meterType.name} image uploaded: $imageUrl")
        return imageUrl
    }

    private suspend fun createMeterReading(
        state: MeterReadingUiState,
        currentUserId: String,
        meterType: MeterType,
        readingValue: String,
        imageUrl: String?
    ): String {
        val readingDoc = MeterReading(
            contractId = state.contractId,
            roomId = state.roomId,
            landlordId = state.landlordId,
            tenantId = currentUserId,
            type = meterType,
            value = readingValue.toInt(),
            imageUrl = imageUrl,
            status = MeterStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        val readingId = meterReadingRepository.add(readingDoc)
        Log.d("MeterReadingVM", "Created ${meterType.name} reading: $readingId")
        return readingId
    }

    private suspend fun notifyLandlord(createdReadingIds: List<String>) {
        for (readingId in createdReadingIds) {
            try {
                val notifyResult = meterReadingCloudFunctions.notifyNewMeterReading(readingId)
                notifyResult.onFailure { e ->
                    Log.e("MeterReadingVM", "Failed to send notification for reading $readingId", e)
                    // Non-critical error, so we don't rethrow or rollback
                }
            } catch (e: Exception) {
                Log.e("MeterReadingVM", "Exception sending notification for reading $readingId", e)
            }
        }
    }

    private suspend fun rollbackSubmission(readingIds: List<String>, imageUrls: List<String>) {
        Log.d("MeterReadingVM", "Rolling back submission...")
        for (readingId in readingIds) {
            try {
                meterReadingRepository.delete(readingId)
                Log.d("MeterReadingVM", "Rolled back reading: $readingId")
            } catch (rollbackError: Exception) {
                Log.e("MeterReadingVM", "Failed to rollback reading $readingId", rollbackError)
            }
        }

        for (imageUrl in imageUrls) {
            try {
                mediaRepository.deleteImage(imageUrl)
                Log.d("MeterReadingVM", "Rolled back image: $imageUrl")
            } catch (rollbackError: Exception) {
                Log.e("MeterReadingVM", "Failed to rollback image $imageUrl", rollbackError)
            }
        }
    }
}
