package com.example.baytro.viewModel.building

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.BuildingStatus
import com.example.baytro.data.MediaRepository
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditBuildingFormState(
    val name: String = "",
    val floor: String = "",
    val address: String = "",
    val status: BuildingStatus = BuildingStatus.ACTIVE,
    val billingDate: String = "",
    val paymentStart: String = "",
    val paymentDue: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val existingImageUrls: List<String> = emptyList()
)

data class BuildingFormErrors(
    val nameError: String? = null,
    val floorError: String? = null,
    val addressError: String? = null,
    val billingError: String? = null,
    val startError: String? = null,
    val dueError: String? = null
)

class EditBuildingVM(
    private val context: Context,
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _building = MutableStateFlow<Building?>(null)
    val building: StateFlow<Building?> = _building

    private val _editUIState = MutableStateFlow<UiState<Building>>(UiState.Loading)
    val editUIState: StateFlow<UiState<Building>> = _editUIState

    private val _formState = MutableStateFlow(EditBuildingFormState())
    val formState: StateFlow<EditBuildingFormState> = _formState.asStateFlow()

    private val _formErrors = MutableStateFlow(BuildingFormErrors())
    val formErrors: StateFlow<BuildingFormErrors> = _formErrors.asStateFlow()

    private var originalFormState: EditBuildingFormState? = null

    fun load(id: String) {
        viewModelScope.launch {
            try {
                val b = buildingRepository.getById(id)
                _building.value = b
                val initialFormState = EditBuildingFormState(
                    name = b?.name ?: "",
                    floor = b?.floor.toString(),
                    address = b?.address ?: "",
                    status = b?.status ?: BuildingStatus.ACTIVE,
                    billingDate = b?.billingDate.toString(),
                    paymentStart = b?.paymentStart.toString(),
                    paymentDue = b?.paymentDue.toString(),
                    selectedImages = emptyList(),
                    existingImageUrls = b?.imageUrls ?: emptyList()
                )
                _formState.value = initialFormState
                originalFormState = initialFormState
                _editUIState.value = UiState.Idle
            } catch (e: Exception) {
                _editUIState.value = UiState.Error(e.message ?: "Failed to load building")
            }
        }
    }

    fun updateField(field: String, value: String) {
        _formState.value = when (field) {
            "name" -> _formState.value.copy(name = value)
            "floor" -> _formState.value.copy(floor = value)
            "address" -> _formState.value.copy(address = value)
            "status" -> _formState.value.copy(status = BuildingStatus.valueOf(value))
            "billingDate" -> _formState.value.copy(billingDate = value)
            "paymentStart" -> _formState.value.copy(paymentStart = value)
            "paymentDue" -> _formState.value.copy(paymentDue = value)
            else -> _formState.value
        }
        validateField(field)
    }

    fun updateImages(images: List<Uri>) {
        _formState.value = _formState.value.copy(selectedImages = images)
    }

    fun hasUnsavedChanges(): Boolean {
        val original = originalFormState ?: return false
        val current = _formState.value
        return original.name != current.name ||
                original.floor != current.floor ||
                original.address != current.address ||
                original.status != current.status ||
                original.billingDate != current.billingDate ||
                original.paymentStart != current.paymentStart ||
                original.paymentDue != current.paymentDue ||
                current.selectedImages.isNotEmpty()
    }

    fun updateExistingImages(urls: List<String>) {
        _formState.value = _formState.value.copy(existingImageUrls = urls)
    }

    private fun validateField(field: String) {
        val state = _formState.value
        _formErrors.value = when (field) {
            "name" -> _formErrors.value.copy(nameError = validateName(state.name))
            "floor" -> _formErrors.value.copy(floorError = validateFloor(state.floor))
            "address" -> _formErrors.value.copy(addressError = validateAddress(state.address))
            "billingDate" -> _formErrors.value.copy(billingError = validateBillingDate(state.billingDate))
            "paymentStart" -> _formErrors.value.copy(startError = validatePaymentStart(state.paymentStart))
            "paymentDue" -> _formErrors.value.copy(dueError = validatePaymentDue(state.paymentDue))
            else -> _formErrors.value
        }
    }

    fun validateAll(): Pair<Boolean, String?> {
        val state = _formState.value
        val errors = BuildingFormErrors(
            nameError = validateName(state.name),
            floorError = validateFloor(state.floor),
            addressError = validateAddress(state.address),
            billingError = validateBillingDate(state.billingDate),
            startError = validatePaymentStart(state.paymentStart),
            dueError = validatePaymentDue(state.paymentDue)
        )
        _formErrors.value = errors

        // Return first error field for focus
        return when {
            errors.nameError != null -> Pair(false, "name")
            errors.floorError != null -> Pair(false, "floor")
            errors.addressError != null -> Pair(false, "address")
            errors.billingError != null -> Pair(false, "billingDate")
            errors.startError != null -> Pair(false, "paymentStart")
            errors.dueError != null -> Pair(false, "paymentDue")
            else -> Pair(true, null)
        }
    }

    private fun validateName(name: String): String? {
        return if (name.isBlank()) "Building name is required" else null
    }

    private fun validateAddress(address: String): String? {
        return if (address.isBlank()) "Address is required" else null
    }

    private fun validateFloor(floor: String): String? {
        if (floor.isBlank()) return "Floor is required"
        val value = floor.toIntOrNull()
        if (value == null || value <= 0) return "Floor must be a positive integer"
        return null
    }

    private fun validateBillingDate(billingDate: String): String? {
        if (billingDate.isBlank()) return "Billing date is required"
        val value = billingDate.toIntOrNull()
        if (value == null || value <= 0) return "Billing date must be a positive integer"
        return null
    }

    private fun validatePaymentStart(paymentStart: String): String? {
        if (paymentStart.isBlank()) return "Payment start is required"
        val value = paymentStart.toIntOrNull()
        if (value == null || value <= 0) return "Payment start must be a positive integer"
        return null
    }

    private fun validatePaymentDue(paymentDue: String): String? {
        if (paymentDue.isBlank()) return "Payment due is required"
        val value = paymentDue.toIntOrNull()
        if (value == null || value <= 0) return "Payment due must be a positive integer"
        return null
    }

    fun saveBuilding() {
        val (isValid, _) = validateAll()
        if (!isValid) return

        val state = _formState.value
        val building = _building.value ?: return

        val updatedBuilding = Building(
            id = building.id,
            name = state.name,
            floor = state.floor.toInt(),
            address = state.address,
            status = state.status,
            billingDate = state.billingDate.toInt(),
            paymentStart = state.paymentStart.toInt(),
            paymentDue = state.paymentDue.toInt(),
            userId = building.userId,
            imageUrls = state.existingImageUrls
        )

        if (state.selectedImages.isNotEmpty()) {
            updateWithImages(updatedBuilding, state.selectedImages)
        } else {
            update(updatedBuilding)
        }
    }

    private fun update(building: Building) {
        viewModelScope.launch {
            _editUIState.value = UiState.Loading
            try {
                buildingRepository.update(building.id, building)
                _editUIState.value = UiState.Success(building)
            } catch (e: Exception) {
                _editUIState.value = UiState.Error(e.message ?: "Failed to update building")
            }
        }
    }

    private fun updateWithImages(building: Building, imageUris: List<Uri>) {
        viewModelScope.launch {
            _editUIState.value = UiState.Loading
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                val limited = imageUris.take(3)
                Log.d("EditBuildingVM", "Starting parallel upload of ${limited.size} images")
                val startTime = System.currentTimeMillis()
                
                Log.d("EditBuildingVM", "Step 1: Compressing ${limited.size} images in parallel...")
                val compressionStartTime = System.currentTimeMillis()
                
                val compressedFiles = limited.mapIndexed { index, uri ->
                    async {
                        Log.d("EditBuildingVM", "Compressing image $index...")
                        val imageStartTime = System.currentTimeMillis()
                        val compressedFile = ImageProcessor.compressImage(context, uri)
                        val compressionTime = System.currentTimeMillis() - imageStartTime
                        Log.d("EditBuildingVM", "Image $index compressed in ${compressionTime}ms, size: ${compressedFile.length()} bytes")
                        compressedFile
                    }
                }.awaitAll()
                
                val totalCompressionTime = System.currentTimeMillis() - compressionStartTime
                Log.d("EditBuildingVM", "All images compressed in ${totalCompressionTime}ms")
                
                Log.d("EditBuildingVM", "Step 2: Uploading ${compressedFiles.size} images in parallel...")
                val uploadStartTime = System.currentTimeMillis()
                
                val urls = compressedFiles.mapIndexed { index, compressedFile ->
                    async {
                        Log.d("EditBuildingVM", "Edit building info")
                        val imageUploadStartTime = System.currentTimeMillis()
                        val compressedUri = Uri.fromFile(compressedFile)
                        val url = mediaRepository.uploadBuildingImage(currentUser.uid, building.id, compressedUri)
                        val uploadTime = System.currentTimeMillis() - imageUploadStartTime
                        Log.d("EditBuildingVM", "Image $index uploaded in ${uploadTime}ms")
                        url
                    }
                }.awaitAll()
                
                val totalUploadTime = System.currentTimeMillis() - uploadStartTime
                Log.d("EditBuildingVM", "All images uploaded in ${totalUploadTime}ms")
                
                val totalTime = System.currentTimeMillis() - startTime
                Log.d("EditBuildingVM", "All ${limited.size} images processed in ${totalTime}ms")
                
                val mergedImageUrls = (building.imageUrls + urls).take(3)
                Log.d("EditBuildingVM", "Merged images: existing=${building.imageUrls.size}, new=${urls.size}, total=${mergedImageUrls.size}")
                
                buildingRepository.update(building.id, building.copy(imageUrls = mergedImageUrls))
                _editUIState.value = UiState.Success(building)
            } catch (e: Exception) {
                _editUIState.value = UiState.Error(e.message ?: "Failed to update building")
            }
        }
    }

    fun clearError() {
        _editUIState.value = UiState.Idle
    }
}
