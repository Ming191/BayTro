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
import com.example.baytro.service.BayTroApiService
import com.example.baytro.utils.cloudFunctions.MeterReadingCloudFunctions
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val electricityConfidence: Float = 0f,
    val waterConfidence: Float = 0f,
    val contractId: String = "",
    val roomId: String = "",
    val buildingId: String = "",
    val landlordId: String = "",
    val roomName: String = "",
    val buildingName: String = ""
)

sealed interface MeterReadingEvent {
    object SubmissionSuccess : MeterReadingEvent
}
sealed interface MeterReadingAction {
    data class UpdateElectricityReading(val reading: String) : MeterReadingAction
    data class UpdateWaterReading(val reading: String) : MeterReadingAction
    data class SelectElectricityPhoto(val uri: Uri) : MeterReadingAction
    data class SelectWaterPhoto(val uri: Uri) : MeterReadingAction
    data class ProcessImage(val uri: Uri, val isForElectricity: Boolean, val context: Context) : MeterReadingAction
    object SubmitReadings : MeterReadingAction
}

class MeterReadingVM(
    private val bayTroApiService: BayTroApiService,
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

    fun initialize(
        contractId: String,
        roomId: String,
        buildingId: String,
        landlordId: String,
        roomName: String,
        buildingName: String
    ) {
        _uiState.update {
            it.copy(
                contractId = contractId,
                roomId = roomId,
                buildingId = buildingId,
                landlordId = landlordId,
                roomName = roomName,
                buildingName = buildingName
            )
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

    fun deleteElectricityPhoto() {
        _uiState.update { it.copy(electricityPhotoUri = null) }
    }

    fun deleteWaterPhoto() {
        _uiState.update { it.copy(waterPhotoUri = null) }
    }

    private fun processImageFromUri(uri: Uri, isForElectricity: Boolean, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }

            if (bitmap == null) {
                _uiState.update { it.copy(isProcessing = false) }
                _errorEvent.emit(SingleEvent("Failed to load image"))
                return@launch
            }
            val result = bayTroApiService.predictMeterReading(bitmap)
            result.onSuccess { response ->
                val meterReading = extractMeterReading(response.detections)
                val avgConfidence = if (response.detections.isNotEmpty()) {
                    response.detections.map { it.conf }.average().toFloat()
                } else {
                    0f
                }
                updateReading(meterReading, isForElectricity)
                _uiState.update {
                    if (isForElectricity) {
                        it.copy(isProcessing = false, electricityConfidence = avgConfidence)
                    } else {
                        it.copy(isProcessing = false, waterConfidence = avgConfidence)
                    }
                }
                val meterType = if (isForElectricity) "Electricity" else "Water"
                if (meterReading.isNotEmpty()) {
                    val confidencePercent = (avgConfidence * 100).toInt()
                    _errorEvent.emit(SingleEvent("$meterType: $meterReading (${confidencePercent}% confident)"))
                } else {
                    _errorEvent.emit(SingleEvent("No reading detected. Please enter manually."))
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isProcessing = false) }
                _errorEvent.emit(SingleEvent("Failed to read meter: ${e.message}"))
            }
        }
    }

    private fun extractMeterReading(detections: List<com.example.baytro.data.MeterDetection>): String {
        return detections.sortedBy { it.x }.joinToString("") { it.label }
    }

    private fun submitReadings() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _errorEvent.emit(SingleEvent("User not authenticated"))
                return@launch
            }

            if (state.electricityReading.isBlank() || state.waterReading.isBlank() || state.electricityPhotoUri == null || state.waterPhotoUri == null) {
                _errorEvent.emit(SingleEvent("Please provide all readings and photos"))
                return@launch
            }
            if (state.contractId.isEmpty() || state.roomId.isEmpty() || state.landlordId.isEmpty()) {
                _errorEvent.emit(SingleEvent("Missing contract information"))
                return@launch
            }

            _uploadProgress.value = 0f

            _uiState.update { it.copy(isSubmitting = true) }
            _uploadProgress.value = 0.1f

            withContext(NonCancellable) {
                var elecImageUrl: String? = null
                var waterImageUrl: String? = null
                var createdReadingId: String? = null

                try {
                    val pathE = "meter_readings/${state.contractId}/${System.currentTimeMillis()}_electricity.jpg"
                    elecImageUrl = mediaRepository.uploadImageFromUri(uri = state.electricityPhotoUri, path = pathE)
                    _uploadProgress.value = 0.4f

                    val pathW = "meter_readings/${state.contractId}/${System.currentTimeMillis()}_water.jpg"
                    waterImageUrl = mediaRepository.uploadImageFromUri(uri = state.waterPhotoUri, path = pathW)
                    _uploadProgress.value = 0.7f

                    val newReadingDoc = MeterReading(
                        contractId = state.contractId,
                        roomId = state.roomId,
                        buildingId = state.buildingId,
                        landlordId = state.landlordId,
                        tenantId = currentUserId,
                        roomName = state.roomName,
                        buildingName = state.buildingName,
                        status = MeterStatus.PENDING,
                        createdAt = Timestamp.now(),
                        electricityValue = state.electricityReading.toInt(),
                        waterValue = state.waterReading.toInt(),
                        electricityImageUrl = elecImageUrl,
                        waterImageUrl = waterImageUrl
                    )
                    createdReadingId = meterReadingRepository.add(newReadingDoc)
                    Log.d("MeterReadingVM", "Created new combined reading: $createdReadingId")
                    _uploadProgress.value = 0.9f

                    meterReadingCloudFunctions.notifyNewMeterReading(createdReadingId)
                    _uploadProgress.value = 0.95f

                    _uiState.update { it.copy(isSubmitting = false) }
                    _uploadProgress.value = 1.0f
                    _event.emit(MeterReadingEvent.SubmissionSuccess)

                } catch (e: Exception) {
                    Log.e("MeterReadingVM", "Error during submission, rolling back...", e)
                    val imageUrlsToRollback = listOfNotNull(elecImageUrl, waterImageUrl)
                    rollbackSubmission(createdReadingId, imageUrlsToRollback)
                    _uploadProgress.value = 0f
                    _uiState.update { it.copy(isSubmitting = false) }
                    val errorMessage = when (e) {
                        is FirebaseFunctionsException -> {
                            e.message ?: e.code.name
                        }
                        else -> e.message ?: "An unknown error occurred"
                    }
                    _errorEvent.emit(SingleEvent(errorMessage))
                }
            }
        }
    }
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