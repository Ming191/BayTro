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
import com.example.baytro.data.service.Metric
import com.example.baytro.data.service.Service
import com.example.baytro.data.service.Status
import com.example.baytro.utils.BuildingValidator
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch

data class AddBuildingFormState(
    val name: String = "",
    val floor: String = "",
    val address: String = "",
    val status: BuildingStatus = BuildingStatus.ACTIVE,
    val billingDate: String = "",
    val paymentStart: String = "",
    val paymentDue: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val buildingServices: List<Service> = emptyList(),

    val nameError: Boolean = false,
    val floorError: Boolean = false,
    val addressError: Boolean = false,
    val billingError: Boolean = false,
    val startError: Boolean = false,
    val dueError: Boolean = false,

    val nameErrorMsg: String? = null,
    val floorErrorMsg: String? = null,
    val addressErrorMsg: String? = null,
    val billingErrorMsg: String? = null,
    val startErrorMsg: String? = null,
    val dueErrorMsg: String? = null
)

class AddBuildingVM(
    private val context: Context,
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    private val _addBuildingUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val addBuildingUIState: StateFlow<AuthUIState> = _addBuildingUIState

    private val _formState = MutableStateFlow(AddBuildingFormState())
    val formState: StateFlow<AddBuildingFormState> = _formState.asStateFlow()

    private val _buildingServices = MutableStateFlow<List<Service>>(emptyList())
    val buildingServices: StateFlow<List<Service>> = _buildingServices

    init {
        createDefaultServices()
    }
    // Form field update methods
    fun updateName(value: String) {
        val validation = BuildingValidator.validateName(value)
        _formState.value = _formState.value.copy(
            name = value,
            nameError = validation.isError,
            nameErrorMsg = validation.message
        )
    }

    fun updateFloor(value: String) {
        val validation = BuildingValidator.validateFloor(value)
        _formState.value = _formState.value.copy(
            floor = value,
            floorError = validation.isError,
            floorErrorMsg = validation.message
        )
    }

    fun updateAddress(value: String) {
        val validation = BuildingValidator.validateAddress(value)
        _formState.value = _formState.value.copy(
            address = value,
            addressError = validation.isError,
            addressErrorMsg = validation.message
        )
    }

    fun updateStatus(value: BuildingStatus) {
        _formState.value = _formState.value.copy(status = value)
    }

    fun updateBillingDate(value: String) {
        val validation = BuildingValidator.validateBillingDate(value)
        _formState.value = _formState.value.copy(
            billingDate = value,
            billingError = validation.isError,
            billingErrorMsg = validation.message
        )
    }

    fun updatePaymentStart(value: String) {
        val validation = BuildingValidator.validatePaymentStart(value)
        _formState.value = _formState.value.copy(
            paymentStart = value,
            startError = validation.isError,
            startErrorMsg = validation.message
        )
    }

    fun updatePaymentDue(value: String) {
        val validation = BuildingValidator.validatePaymentDue(value)
        _formState.value = _formState.value.copy(
            paymentDue = value,
            dueError = validation.isError,
            dueErrorMsg = validation.message
        )
    }

    fun onBuildingServicesChange(service: Service) {
        Log.d("AddBuildingVM", "onBuildingServicesChange: $service")
        val updateBuildingServices = _buildingServices.value.toMutableList()
        updateBuildingServices.add(service)
        _buildingServices.value = updateBuildingServices
        _formState.value = _formState.value.copy(buildingServices = updateBuildingServices)
    }

    fun updateSelectedImages(images: List<Uri>) {
        _formState.value = _formState.value.copy(selectedImages = images)
    }

    fun validateAndSubmit() {
        val state = _formState.value

        // Reset errors
        _formState.value = state.copy(
            nameError = false,
            floorError = false,
            addressError = false,
            billingError = false,
            startError = false,
            dueError = false,
            nameErrorMsg = null,
            floorErrorMsg = null,
            addressErrorMsg = null,
            billingErrorMsg = null,
            startErrorMsg = null,
            dueErrorMsg = null
        )

        // Validate all fields
        val nameValidation = BuildingValidator.validateName(state.name)
        val floorValidation = BuildingValidator.validateFloor(state.floor)
        val addressValidation = BuildingValidator.validateAddress(state.address)
        val billingValidation = BuildingValidator.validateBillingDate(state.billingDate)
        val startValidation = BuildingValidator.validatePaymentStart(state.paymentStart)
        val dueValidation = BuildingValidator.validatePaymentDue(state.paymentDue)

        // Update state with validation results
        _formState.value = _formState.value.copy(
            nameError = nameValidation.isError,
            nameErrorMsg = nameValidation.message,
            floorError = floorValidation.isError,
            floorErrorMsg = floorValidation.message,
            addressError = addressValidation.isError,
            addressErrorMsg = addressValidation.message,
            billingError = billingValidation.isError,
            billingErrorMsg = billingValidation.message,
            startError = startValidation.isError,
            startErrorMsg = startValidation.message,
            dueError = dueValidation.isError,
            dueErrorMsg = dueValidation.message
        )

        // If all validations pass, proceed with submission
        val allValid = !nameValidation.isError && !floorValidation.isError &&
                !addressValidation.isError && !billingValidation.isError &&
                !startValidation.isError && !dueValidation.isError

        if (allValid) {
            val building = Building(
                id = "",
                name = state.name,
                floor = state.floor.toInt(),
                address = state.address,
                status = state.status,
                billingDate = state.billingDate.toInt(),
                paymentStart = state.paymentStart.toInt(),
                paymentDue = state.paymentDue.toInt(),
                imageUrls = emptyList()
            )

            if (state.selectedImages.isEmpty()) {
                addBuilding(building)
            } else {
                addBuildingWithImages(building, state.selectedImages)
            }
        }
    }

    private fun createDefaultServices() {
        val services =  listOf(
            Service(
                name = "Water",
                price = "18000",
                metric = Metric.M3,
                status = Status.ACTIVE
            ),
            Service(
                name = "Electricity",
                price = "4000",
                metric = Metric.KWH,
                status = Status.ACTIVE
            )
        )
        _formState.value = _formState.value.copy(
            buildingServices = services
        )
        _buildingServices.value = services
    }

    fun addBuilding(building: Building) {
        viewModelScope.launch {
            _addBuildingUIState.value = AuthUIState.Loading
            _formState.value = _formState.value.copy(
                name = building.name,
                floor = building.floor.toString(),
                address = building.address,
                status = BuildingStatus.ACTIVE,
                billingDate = building.billingDate.toString(),
                paymentStart = building.paymentStart.toString(),
                paymentDue = building.paymentDue.toString(),
            )
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                
                val buildingWithDefaults = building.copy(
                    userId = currentUser.uid
                )
                val newId = buildingRepository.add(buildingWithDefaults)
                buildingServices.value.forEach { services ->
                    buildingRepository.addServiceToBuilding(newId, services)
                }
                // Update the document to include its own ID
                buildingRepository.updateFields(newId, mapOf("id" to newId))
                
                Log.d("AddBuildingVM", "Building created with default services")

                _addBuildingUIState.value = AuthUIState.Success(currentUser)
            } catch (e: Exception) {
                _addBuildingUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun addBuildingWithImages(building: Building, imageUris: List<Uri>) {
        viewModelScope.launch {
            _addBuildingUIState.value = AuthUIState.Loading
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")

                val buildingWithDefaults = building.copy(
                    userId = currentUser.uid,
                    imageUrls = emptyList(),
                    services = buildingServices.value
                )
                val newId = buildingRepository.add(buildingWithDefaults)

                buildingRepository.updateFields(newId, mapOf("id" to newId))
                Log.d("AddBuildingVM", "Building created with ID: $newId")

                val limitedUris = imageUris.take(3)
                Log.d("AddBuildingVM", "Starting optimized upload of ${limitedUris.size} images")
                val startTime = System.currentTimeMillis()
                
                // Tối ưu: Compress và upload song song cho từng ảnh
                val uploadedUrls = limitedUris.mapIndexed { index, uri ->
                    async {
                        val imageStartTime = System.currentTimeMillis()
                        Log.d("AddBuildingVM", "Processing image $index...")
                        
                        try {
                            val compressedFile = ImageProcessor.compressImage(context, uri)
                            val compressionTime = System.currentTimeMillis() - imageStartTime
                            Log.d("AddBuildingVM", "Image $index compressed in ${compressionTime}ms, size: ${compressedFile.length()} bytes")
                            
                            // Upload ngay
                            val uploadStartTime = System.currentTimeMillis()
                            val compressedUri = Uri.fromFile(compressedFile)
                            val url = mediaRepository.uploadBuildingImage(currentUser.uid, newId, compressedUri)
                            val uploadTime = System.currentTimeMillis() - uploadStartTime
                            Log.d("AddBuildingVM", "Image $index uploaded in ${uploadTime}ms")
                            
                            // Cleanup temp file
                            compressedFile.delete()
                            
                            val totalImageTime = System.currentTimeMillis() - imageStartTime
                            Log.d("AddBuildingVM", "Image $index total time: ${totalImageTime}ms")
                            
                            url
                        } catch (e: Exception) {
                            Log.e("AddBuildingVM", "Failed to process image $index", e)
                            throw e
                        }
                    }
                }.awaitAll()
                
                val totalTime = System.currentTimeMillis() - startTime
                Log.d("AddBuildingVM", "All ${limitedUris.size} images processed in ${totalTime}ms")
                
                // Cập nhật building với URLs
                if (uploadedUrls.isNotEmpty()) {
                    buildingRepository.updateFields(newId, mapOf("imageUrls" to uploadedUrls))
                    Log.d("AddBuildingVM", "Building updated with ${uploadedUrls.size} image URLs")
                }

                _addBuildingUIState.value = AuthUIState.Success(currentUser)
                Log.d("AddBuildingVM", "Add building completed successfully")

            } catch (e: Exception) {
                Log.e("AddBuildingVM", "Failed to add building with images", e)
                _addBuildingUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
