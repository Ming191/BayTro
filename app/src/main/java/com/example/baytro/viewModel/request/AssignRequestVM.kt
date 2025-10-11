package com.example.baytro.viewModel.request

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.request.Request
import com.example.baytro.data.request.RequestRepository
import com.example.baytro.data.request.RequestStatus
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.utils.isError
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssignRequestFormState(
    val assigneeName: String = "",
    val assigneeNameError: ValidationResult = ValidationResult.Success,
    val assigneePhoneNumber: String = "",
    val assigneePhoneNumberError: ValidationResult = ValidationResult.Success,
    val selectedPhotos: List<Uri> = emptyList(),
    val existingImageUrls: List<String> = emptyList()
)

class AssignRequestVM(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(AssignRequestFormState())
    val formState: StateFlow<AssignRequestFormState> = _formState.asStateFlow()

    private val _assignUiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val assignUiState: StateFlow<UiState<String>> = _assignUiState.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0.0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private var currentRequestId: String? = null
    private var currentRequest: Request? = null

    fun loadRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val request = requestRepository.getById(requestId)
                if (request != null) {
                    currentRequestId = requestId
                    currentRequest = request
                    _formState.value = _formState.value.copy(
                        existingImageUrls = request.imageUrls
                    )
                }
            } catch (e: Exception) {
                _assignUiState.value = UiState.Error("Failed to load request: ${e.message}")
            }
        }
    }

    fun onAssigneeNameChange(name: String) {
        _formState.value = _formState.value.copy(
            assigneeName = name,
            assigneeNameError = ValidationResult.Success
        )
    }

    fun onAssigneePhoneNumberChange(phoneNumber: String) {
        _formState.value = _formState.value.copy(
            assigneePhoneNumber = phoneNumber,
            assigneePhoneNumberError = ValidationResult.Success
        )
    }

    fun onPhotosSelected(photos: List<Uri>) {
        _formState.value = _formState.value.copy(selectedPhotos = photos)
    }

    private fun validateForm(): Boolean {
        val state = _formState.value

        val assigneeNameError = Validator.validateNonEmpty(state.assigneeName, "assignee name")
        val assigneePhoneError = Validator.validatePhoneNumber(state.assigneePhoneNumber)

        _formState.value = state.copy(
            assigneeNameError = assigneeNameError,
            assigneePhoneNumberError = assigneePhoneError
        )

        return !assigneeNameError.isError() && !assigneePhoneError.isError()
    }

    fun assignRequest() {
        if (!validateForm()) return

        val requestId = currentRequestId ?: return
        val state = _formState.value

        viewModelScope.launch {
            try {
                _assignUiState.value = UiState.Loading
                _uploadProgress.value = 0.0f

                // Step 1: Start assignment
                _uploadProgress.value = 0.3f

                // Update request with assignee information and change status to IN_PROGRESS
                val updateFields = mapOf(
                    "assigneeName" to state.assigneeName,
                    "assigneePhoneNumber" to state.assigneePhoneNumber,
                    "status" to RequestStatus.IN_PROGRESS,
                    "acceptedDate" to java.time.LocalDateTime.now().toString()
                )

                // Step 2: Updating database
                _uploadProgress.value = 0.6f
                requestRepository.updateFields(requestId, updateFields)

                // Step 3: Complete
                _uploadProgress.value = 1.0f
                _assignUiState.value = UiState.Success("Request assigned successfully")
            } catch (e: Exception) {
                _uploadProgress.value = 0.0f
                _assignUiState.value = UiState.Error("Failed to assign request: ${e.message}")
            }
        }
    }

    fun resetState() {
        _formState.value = AssignRequestFormState()
        _assignUiState.value = UiState.Idle
        _uploadProgress.value = 0.0f
        currentRequestId = null
        currentRequest = null
    }
}
