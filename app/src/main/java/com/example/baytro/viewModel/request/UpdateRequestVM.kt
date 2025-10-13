package com.example.baytro.viewModel.request

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.request.Request
import com.example.baytro.data.request.RequestRepository
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class UpdateRequestVM(
    private val requestRepository: RequestRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository,
    private val requestId: String
) : ViewModel() {

    private val _updateRequestUIState = MutableStateFlow<UiState<Request>>(UiState.Idle)
    val updateRequestUIState: StateFlow<UiState<Request>> = _updateRequestUIState.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0.0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _formState = MutableStateFlow(UpdateRequestFormState())
    val formState: StateFlow<UpdateRequestFormState> = _formState.asStateFlow()

    init {
        loadRequest()
    }

    private fun loadRequest() {
        viewModelScope.launch {
            try {
                _formState.value = _formState.value.copy(isLoading = true)
                val request = requestRepository.getById(requestId)
                if (request != null) {
                    _formState.value = _formState.value.copy(
                        title = request.title,
                        description = request.description,
                        scheduledDate = request.scheduledDate,
                        existingImageUrls = request.imageUrls,
                        isLoading = false
                    )
                } else {
                    _updateRequestUIState.value = UiState.Error("Request not found")
                    _formState.value = _formState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("UpdateRequestVM", "Error loading request", e)
                _updateRequestUIState.value = UiState.Error(e.message ?: "Error loading request")
                _formState.value = _formState.value.copy(isLoading = false)
            }
        }
    }

    fun updateTitle(value: String) {
        _formState.value = _formState.value.copy(
            title = value,
            titleError = ValidationResult.Success
        )
    }

    fun updateDescription(value: String) {
        _formState.value = _formState.value.copy(
            description = value,
            descriptionError = ValidationResult.Success
        )
    }

    fun updateScheduledDate(value: String) {
        _formState.value = _formState.value.copy(
            scheduledDate = value,
            scheduledDateError = ValidationResult.Success
        )
    }

    fun updateSelectedPhotos(photos: List<Uri>) {
        _formState.value = _formState.value.copy(
            selectedPhotos = photos,
            photoError = ValidationResult.Success
        )
    }

    fun updateExistingImageUrls(urls: List<String>) {
        _formState.value = _formState.value.copy(existingImageUrls = urls)
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val state = _formState.value

        val titleError = if (state.title.isBlank()) {
            isValid = false
            ValidationResult.Error("Title cannot be empty")
        } else {
            ValidationResult.Success
        }

        val descriptionError = if (state.description.isBlank()) {
            isValid = false
            ValidationResult.Error("Description cannot be empty")
        } else {
            ValidationResult.Success
        }

        val scheduledDateError = if (state.scheduledDate.isBlank()) {
            isValid = false
            ValidationResult.Error("Scheduled date cannot be empty")
        } else {
            ValidationResult.Success
        }

        val totalImages = state.selectedPhotos.size + state.existingImageUrls.size
        val photoError = if (totalImages == 0) {
            isValid = false
            ValidationResult.Error("At least one photo is required")
        } else {
            ValidationResult.Success
        }

        _formState.value = state.copy(
            titleError = titleError,
            descriptionError = descriptionError,
            scheduledDateError = scheduledDateError,
            photoError = photoError
        )

        return isValid
    }

    fun updateRequest(context: Context) {
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _updateRequestUIState.value = UiState.Loading
            _uploadProgress.value = 0.0f
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")

                val state = _formState.value
                val newPhotos = state.selectedPhotos.take(5)
                val hasNewImages = newPhotos.isNotEmpty()

                val imageUrls = state.existingImageUrls.toMutableList()

                if (hasNewImages) {
                    val totalSteps = newPhotos.size + 1
                    var completedSteps: Int
                    _uploadProgress.value = 0.05f

                    Log.d("UpdateRequestVM", "Starting upload of ${newPhotos.size} new images")
                    val uploadedUrlsFlow = channelFlow {
                        val uploaded = mutableListOf<String>()
                        newPhotos.mapIndexed { index, uri ->
                            launch {
                                val timestamp = System.currentTimeMillis()
                                val compressedFile = ImageProcessor.compressImage(context, uri, 1024, 50)
                                val uploadedUrl = mediaRepository.uploadUserImage(
                                    currentUser.uid,
                                    Uri.fromFile(compressedFile),
                                    "request_images",
                                    "$requestId-$timestamp-$index.jpg"
                                )

                                val currentUrlsSnapshot = synchronized(uploaded) {
                                    uploaded.add(uploadedUrl)
                                    uploaded.toList()
                                }
                                send(currentUrlsSnapshot)
                            }
                        }
                    }

                    uploadedUrlsFlow
                        .onEach { urls ->
                            completedSteps = urls.size
                            _uploadProgress.value = completedSteps.toFloat() / totalSteps
                            Log.d("UpdateRequestVM", "Progress: ${(_uploadProgress.value * 100).toInt()}%")

                            if (urls.size == newPhotos.size) {
                                imageUrls.addAll(urls)
                            }
                        }
                        .catch { e ->
                            Log.e("UpdateRequestVM", "Error during image upload flow: ${e.message}", e)
                            throw e
                        }
                        .launchIn(this)
                        .join()
                }

                val updateFields = mapOf(
                    "title" to state.title,
                    "description" to state.description,
                    "scheduledDate" to state.scheduledDate,
                    "imageUrls" to imageUrls
                )

                requestRepository.updateFields(requestId, updateFields)
                Log.d("UpdateRequestVM", "Request updated successfully")

                delay(500L)
                val updatedRequest = requestRepository.getById(requestId)
                _updateRequestUIState.value = UiState.Success(updatedRequest!!)

            } catch (e: Exception) {
                Log.e("UpdateRequestVM", "Error updating request: ${e.message}", e)
                _updateRequestUIState.value = UiState.Error(e.message ?: "An unknown error occurred")
                _uploadProgress.value = 0.0f
            }
        }
    }

    fun resetState() {
        _updateRequestUIState.value = UiState.Idle
        _uploadProgress.value = 0.0f
    }
}

