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
import com.example.baytro.data.request.RequestStatus
import com.example.baytro.data.contract.ContractRepository
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
import dev.gitlive.firebase.firestore.Timestamp

class AddRequestVM(
    private val requestRepository: RequestRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository,
    private val contractRepository: ContractRepository
) : ViewModel() {

    private val _addRequestUIState = MutableStateFlow<UiState<Request>>(UiState.Idle)
    val addRequestUIState: StateFlow<UiState<Request>> = _addRequestUIState.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0.0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _formState = MutableStateFlow(AddRequestFormState())
    val formState: StateFlow<AddRequestFormState> = _formState.asStateFlow()

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

        val photoError = if (state.selectedPhotos.isEmpty()) {
            isValid = false
            ValidationResult.Error("You can upload up to 5 photos")
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

    fun addRequest(context: Context) {
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _addRequestUIState.value = UiState.Loading
            _uploadProgress.value = 0.0f
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")

                val state = _formState.value

                val limitedUris = state.selectedPhotos.take(5)

                val hasImages = limitedUris.isNotEmpty()
                val totalSteps = 1 + 1 + (if (hasImages) limitedUris.size + 1 else 0)
                var completedSteps = 0
                _uploadProgress.value = 0.05f

                val activeContract = contractRepository.getActiveContract(currentUser.uid)
                val roomId = activeContract?.roomId ?: throw IllegalStateException("No active contract found")

                completedSteps++
                _uploadProgress.value = completedSteps.toFloat() / totalSteps

                val request = Request(
                    tenantId = currentUser.uid,
                    landlordId = activeContract.landlordId,
                    roomId = roomId,
                    status = RequestStatus.PENDING,
                    createdAt = Timestamp.now(),
                    scheduledDate = state.scheduledDate,
                    imageUrls = emptyList(),
                    description = state.description,
                    title = state.title,
                    assigneeName = null,
                    completionDate = null,
                    acceptedDate = null,
                    assigneePhoneNumber = null,
                )
                val newId = requestRepository.add(request)
                Log.d("AddRequestVM", "Request created with ID: $newId")

                completedSteps++
                _uploadProgress.value = completedSteps.toFloat() / totalSteps

                if (!hasImages) {
                    _addRequestUIState.value = UiState.Success(request.copy(id = newId))
                    return@launch
                }

                Log.d("AddRequestVM", "Starting upload of ${limitedUris.size} images")
                val uploadedUrlsFlow = channelFlow {
                    val uploaded = mutableListOf<String>()
                    limitedUris.mapIndexed { index, uri ->
                        launch {
                            val compressedFile = ImageProcessor.compressImage(context, uri, 1024, 50)
                            val uploadedUrl = mediaRepository.uploadUserImage(currentUser.uid, Uri.fromFile(compressedFile), "request_images", "$newId-$index.jpg")

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
                        completedSteps = 2 + urls.size
                        _uploadProgress.value = completedSteps.toFloat() / totalSteps
                        Log.d("AddRequestVM", "Progress: ${(_uploadProgress.value * 100).toInt()}%")

                        if (urls.size == limitedUris.size) {
                            requestRepository.updateFields(newId, mapOf("imageUrls" to urls))

                            completedSteps++
                            _uploadProgress.value = completedSteps.toFloat() / totalSteps
                            delay(800L)
                            _addRequestUIState.value = UiState.Success(request.copy(id = newId, imageUrls = urls))
                        }
                    }
                    .catch { e ->
                        Log.e("AddRequestVM", "Error during image upload flow: ${e.message}", e)
                        _addRequestUIState.value = UiState.Error(e.message ?: "Image upload failed")
                        _uploadProgress.value = 0.0f
                    }
                    .launchIn(viewModelScope)

            } catch (e: Exception) {
                Log.e("AddRequestVM", "Error adding request with images: ${e.message}", e)
                _addRequestUIState.value = UiState.Error(e.message ?: "An unknown error occurred")
                _uploadProgress.value = 0.0f
            }
        }
    }

    fun resetState() {
        _addRequestUIState.value = UiState.Idle
        _formState.value = AddRequestFormState()
        _uploadProgress.value = 0.0f
    }
}