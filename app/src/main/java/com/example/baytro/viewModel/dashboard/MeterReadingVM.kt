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
// IMPORTANT: Make sure this import points to your updated MeterReading data class
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.service.MeterReadingApiService
import com.example.baytro.service.MeterReadingCloudFunctions
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// === 1. Redefined UI State: No more Maps or MeterType ===
data class MeterReadingUiState(
    val isProcessing: Boolean = false,
    val isSubmitting: Boolean = false,
    val electricityReading: String = "",
    val waterReading: String = "",
    val electricityPhotoUri: Uri? = null,
    val waterPhotoUri: Uri? = null,
    val recognizedText: String = "",
    val capturedImage: Bitmap? = null,
    val detectionConfidence: Float = 0f,
    val contractId: String = "",
    val roomId: String = "",
    val landlordId: String = ""
)

sealed interface MeterReadingEvent {
    object SubmissionSuccess : MeterReadingEvent
}

// === 2. Redefined Actions: Specific actions for each field ===
sealed interface MeterReadingAction {
    data class UpdateElectricityReading(val reading: String) : MeterReadingAction
    data class UpdateWaterReading(val reading: String) : MeterReadingAction
    data class SelectElectricityPhoto(val uri: Uri) : MeterReadingAction
    data class SelectWaterPhoto(val uri: Uri) : MeterReadingAction
    data class ProcessImage(val uri: Uri, val isForElectricity: Boolean, val context: Context) : MeterReadingAction
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
            is MeterReadingAction.UpdateElectricityReading -> updateReading(action.reading, isForElectricity = true)
            is MeterReadingAction.UpdateWaterReading -> updateReading(action.reading, isForElectricity = false)
            is MeterReadingAction.SelectElectricityPhoto -> setSelectedPhoto(action.uri, isForElectricity = true)
            is MeterReadingAction.SelectWaterPhoto -> setSelectedPhoto(action.uri, isForElectricity = false)
            is MeterReadingAction.ProcessImage -> processImageFromUri(action.uri, action.isForElectricity, action.context)
            is MeterReadingAction.SubmitReadings -> submitReadings()
        }
    }

    fun initialize(contractId: String, roomId: String, landlordId: String) {
        _uiState.update {
            it.copy(contractId = contractId, roomId = roomId, landlordId = landlordId)
        }
    }

    private fun updateReading(reading: String, isForElectricity: Boolean) {
        val filtered = reading.filter { it.isDigit() }
        _uiState.update {
            if (isForElectricity) it.copy(electricityReading = filtered)
            else it.copy(waterReading = filtered)
        }
    }

    private fun setSelectedPhoto(uri: Uri, isForElectricity: Boolean) {
        _uiState.update {
            if (isForElectricity) it.copy(electricityPhotoUri = uri)
            else it.copy(waterPhotoUri = uri)
        }
    }

    private fun processImageFromUri(uri: Uri, isForElectricity: Boolean, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            // The internal logic for image processing remains largely the same.
            // The key change is how the result is applied.
            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }

            if (bitmap == null) {
                _uiState.update { it.copy(isProcessing = false) }
                _errorEvent.emit(SingleEvent("Failed to load image"))
                return@launch
            }

            val result = meterReadingApiService.predictMeterReading(bitmap)
            result.onSuccess { response ->
                val meterReading = extractMeterReading(response.detections)
                // Use the simplified updateReading function
                updateReading(meterReading, isForElectricity)
                _uiState.update { it.copy(isProcessing = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isProcessing = false) }
                _errorEvent.emit(SingleEvent("Failed to read meter: ${e.message}"))
            }
        }
    }

    private fun extractMeterReading(detections: List<com.example.baytro.data.MeterDetection>): String {
        return detections.sortedBy { it.x }.joinToString("") { it.label }
    }

    // === 3. Rewritten submission logic ===
    private fun submitReadings() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _errorEvent.emit(SingleEvent("User not authenticated"))
                return@launch
            }

            // --- Step 1: Validate input ---
            if (state.electricityReading.isBlank() || state.waterReading.isBlank()) {
                _errorEvent.emit(SingleEvent("Please provide both electricity and water readings."))
                return@launch
            }
            if (state.contractId.isEmpty() || state.roomId.isEmpty() || state.landlordId.isEmpty()) {
                _errorEvent.emit(SingleEvent("Missing contract information"))
                return@launch
            }

            _uiState.update { it.copy(isSubmitting = true) }
            _uploadProgress.value = 0.1f

            withContext(NonCancellable) {
                var elecImageUrl: String? = null
                var waterImageUrl: String? = null
                var createdReadingId: String? = null

                try {
                    // --- Step 2: Upload images (if selected) ---
                    if (state.electricityPhotoUri != null) {
                        val path = "meter_readings/${state.contractId}/${System.currentTimeMillis()}_electricity.jpg"
                        elecImageUrl = mediaRepository.uploadImageFromUri(uri = state.electricityPhotoUri, path = path)
                    }
                    _uploadProgress.value = 0.4f

                    if (state.waterPhotoUri != null) {
                        val path = "meter_readings/${state.contractId}/${System.currentTimeMillis()}_water.jpg"
                        waterImageUrl = mediaRepository.uploadImageFromUri(uri = state.waterPhotoUri, path = path)
                    }
                    _uploadProgress.value = 0.7f

                    // --- Step 3: Create a single document ---
                    val newReadingDoc = MeterReading(
                        contractId = state.contractId,
                        roomId = state.roomId,
                        landlordId = state.landlordId,
                        tenantId = currentUserId,
                        status = MeterStatus.PENDING,
                        createdAt = System.currentTimeMillis(),
                        electricityValue = state.electricityReading.toInt(),
                        waterValue = state.waterReading.toInt(),
                        electricityImageUrl = elecImageUrl,
                        waterImageUrl = waterImageUrl
                    )
                    createdReadingId = meterReadingRepository.add(newReadingDoc)
                    Log.d("MeterReadingVM", "Created new combined reading: $createdReadingId")
                    _uploadProgress.value = 0.9f

                    // --- Step 4: Send a single notification ---
                    meterReadingCloudFunctions.notifyNewMeterReading(createdReadingId)
                    _uploadProgress.value = 0.95f

                    // --- Step 5: Finalize ---
                    _uiState.update { it.copy(isSubmitting = false) }
                    _uploadProgress.value = 1.0f
                    _event.emit(MeterReadingEvent.SubmissionSuccess)

                } catch (e: Exception) {
                    Log.e("MeterReadingVM", "Error during submission, rolling back...", e)
                    // --- Step 6: Rollback on failure ---
                    val imageUrlsToRollback = listOfNotNull(elecImageUrl, waterImageUrl)
                    rollbackSubmission(createdReadingId, imageUrlsToRollback)
                    _uploadProgress.value = 0f
                    _uiState.update { it.copy(isSubmitting = false) }
                    _errorEvent.emit(SingleEvent("Submission failed: ${e.message}"))
                }
            }
        }
    }

    // --- 4. Simplified rollback logic ---
    private suspend fun rollbackSubmission(readingId: String?, imageUrls: List<String>) {
        Log.d("MeterReadingVM", "Rolling back submission...")
        readingId?.let {
            try {
                meterReadingRepository.delete(it)
                Log.d("MeterReadingVM", "Rolled back reading: $it")
            } catch (e: Exception) {
                Log.e("MeterReadingVM", "Failed to rollback reading $it", e)
            }
        }
        imageUrls.forEach { url ->
            try {
                mediaRepository.deleteImage(url)
                Log.d("MeterReadingVM", "Rolled back image: $url")
            } catch (e: Exception) {
                Log.e("MeterReadingVM", "Failed to rollback image $url", e)
            }
        }
    }
}