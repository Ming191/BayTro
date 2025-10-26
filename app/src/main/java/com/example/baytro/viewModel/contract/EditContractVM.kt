package com.example.baytro.viewModel.contract

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.service.Service
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditContractVM(
    private val mediaRepo: MediaRepository,
    private val contractRepo: ContractRepository,
    private val roomRepo: RoomRepository,
    private val buildingRepo: BuildingRepository,
    private val auth: AuthRepository
) : ViewModel() {
    companion object {
        private const val TAG = "EditContractVM"
    }

    private val _editContractUiState = MutableStateFlow<UiState<Contract>>(UiState.Idle)
    val editContractUiState: StateFlow<UiState<Contract>> = _editContractUiState

    private val _editContractFormState = MutableStateFlow(EditContractFormState())
    val editContractFormState: StateFlow<EditContractFormState> = _editContractFormState

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _buildingServices = MutableStateFlow<List<Service>>(emptyList())
    val buildingServices: StateFlow<List<Service>> = _buildingServices

    private val _extraServices = MutableStateFlow<List<Service>>(emptyList())
    val extraServices: StateFlow<List<Service>> = _extraServices

    fun loadContract(contractId: String) {
        Log.d(TAG, "loadContract: contractId=$contractId")
        _loading.value = true
        viewModelScope.launch {
            try {
                val contract = contractRepo.getById(contractId)
                if (contract == null) {
                    _editContractUiState.value = UiState.Error("Contract not found")
                    _loading.value = false
                    return@launch
                }

                // Fetch building and room details
                val room = roomRepo.getById(contract.roomId)
                val building = room?.let { buildingRepo.getById(it.buildingId) }

                _editContractFormState.value = EditContractFormState(
                    contractId = contractId,
                    selectedBuilding = building,
                    selectedRoom = room,
                    startDate = contract.startDate,
                    endDate = contract.endDate,
                    rentalFee = contract.rentalFee.toString(),
                    deposit = contract.deposit.toString(),
                    status = contract.status,
                    existingPhotosURL = contract.photosURL,
                    contractNumber = contract.contractNumber,
                    tenantList = contract.tenantIds
                )

                // Load services for the building and room
                loadServices(contract.roomId)

                _loading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "loadContract: error", e)
                _editContractUiState.value = UiState.Error(e.message ?: "Error loading contract")
                _loading.value = false
            }
        }
    }

    fun clearError() {
        Log.d(TAG, "clearError: setting UiState to Idle")
        _editContractUiState.value = UiState.Idle
    }

    private fun loadServices(roomId: String) {
        viewModelScope.launch {
            try {
                val room = roomRepo.getById(roomId)
                if (room != null) {
                    val building = buildingRepo.getById(room.buildingId)
                    if (building != null) {
                        _buildingServices.value = buildingRepo.getServicesByBuildingId(building.id)
                    } else {
                        Log.e(TAG, "loadServices: building not found")
                    }
                    _extraServices.value = roomRepo.getExtraServicesByRoomId(room.id)
                } else {
                    Log.e(TAG, "loadServices: room not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadServices: error fetching services", e)
            }
        }
    }

    fun onStartDateChange(startDate: String) {
        Log.d(TAG, "onStartDateChange: startDate=$startDate")
        _editContractFormState.value = _editContractFormState.value.copy(startDate = startDate)
    }

    fun onEndDateChange(endDate: String) {
        Log.d(TAG, "onEndDateChange: endDate=$endDate")
        _editContractFormState.value = _editContractFormState.value.copy(endDate = endDate)
    }

    fun onRentalFeeChange(rentalFee: String) {
        Log.d(TAG, "onRentalFeeChange: rentalFee=$rentalFee")
        _editContractFormState.value = _editContractFormState.value.copy(rentalFee = rentalFee)
    }

    fun onDepositChange(deposit: String) {
        Log.d(TAG, "onDepositChange: deposit=$deposit")
        _editContractFormState.value = _editContractFormState.value.copy(deposit = deposit)
    }

    fun onStatusChange(status: Status) {
        Log.d(TAG, "onStatusChange: status=$status")
        _editContractFormState.value = _editContractFormState.value.copy(status = status)
    }

    fun onPhotosChange(photos: List<Uri>) {
        Log.d(TAG, "onPhotosChange: photosCount=${photos.size}")
        _editContractFormState.value = _editContractFormState.value.copy(selectedPhotos = photos)
    }

    private fun validateInput(): Boolean {
        Log.d(TAG, "validateInput: start")
        val formState = _editContractFormState.value
        val startDateValidator = Validator.validateNonEmpty(formState.startDate, "Start Date")
        val endDateValidator = if (Validator.validateNonEmpty(formState.endDate, "End Date") == ValidationResult.Success) {
            if (startDateValidator == ValidationResult.Success) {
                Validator.validateStartEndDate(formState.startDate, formState.endDate)
            } else {
                ValidationResult.Success
            }
        } else {
            Validator.validateNonEmpty(formState.endDate, "End Date")
        }
        val rentalFeeValidator = Validator.validateInteger(formState.rentalFee, "Rental Fee")
        val depositValidator = Validator.validateInteger(formState.deposit, "Deposit")

        val allResults = listOf(
            startDateValidator,
            endDateValidator,
            rentalFeeValidator,
            depositValidator,
        )

        val isValid = allResults.all { it == ValidationResult.Success }

        if (!isValid) {
            val errors = buildList {
                if (startDateValidator != ValidationResult.Success) add("startDate=$startDateValidator")
                if (endDateValidator != ValidationResult.Success) add("endDate=$endDateValidator")
                if (rentalFeeValidator != ValidationResult.Success) add("rentalFee=$rentalFeeValidator")
                if (depositValidator != ValidationResult.Success) add("deposit=$depositValidator")
            }
            Log.w(TAG, "validateInput: validation failed -> ${errors.joinToString(", ")}")
        }

        Log.d(TAG, "validateInput: result isValid=$isValid")
        _editContractFormState.value = formState.copy(
            startDateError = startDateValidator,
            endDateError = endDateValidator,
            rentalFeeError = rentalFeeValidator,
            depositError = depositValidator,
        )
        return isValid
    }

    fun onSubmit(context: Context) {
        Log.d(TAG, "onSubmit: start")
        if (!validateInput()) {
            Log.w(TAG, "onSubmit: validation failed; aborting submit")
            return
        }

        val formState = _editContractFormState.value
        val currentUser = auth.getCurrentUser()
        if (currentUser == null) {
            Log.w(TAG, "onSubmit: no authenticated user; aborting submit")
            _editContractUiState.value = UiState.Error("No authenticated user. Please log in again.")
            return
        }

        Log.d(TAG, "onSubmit: updating contract with ${formState.selectedPhotos.size} new photos")
        viewModelScope.launch {
            _editContractUiState.value = UiState.Loading
            try {
                val newPhotoUrls = mutableListOf<String>()
                formState.selectedPhotos.forEachIndexed { index, photoUri ->
                    Log.d(TAG, "onSubmit: processing photo $index")
                    val compressedFile = ImageProcessor.compressImage(
                        context = context,
                        uri = photoUri,
                        maxWidth = 1080,
                        quality = 85
                    )

                    val photoUrl = mediaRepo.uploadContractPhoto(
                        contractId = formState.contractId,
                        photoIndex = index,
                        imageFile = compressedFile
                    )
                    newPhotoUrls.add(photoUrl)
                    Log.d(TAG, "onSubmit: photo $index uploaded: $photoUrl")
                }

                // Combine existing and new photo URLs
                val allPhotoUrls = formState.existingPhotosURL + newPhotoUrls

                // Update contract
                val updatedContract = Contract(
                    id = formState.contractId,
                    tenantIds = formState.tenantList,
                    roomId = formState.selectedRoom?.id ?: "",
                    startDate = formState.startDate,
                    endDate = formState.endDate,
                    rentalFee = formState.rentalFee.toInt(),
                    deposit = formState.deposit.toInt(),
                    status = formState.status,
                    photosURL = allPhotoUrls,
                    buildingId = formState.selectedBuilding?.id ?: "",
                    landlordId = currentUser.uid,
                    contractNumber = formState.contractNumber
                )

                contractRepo.update(formState.contractId, updatedContract)
                Log.d(TAG, "onSubmit: contract updated successfully")

                val finalContract = contractRepo.getById(formState.contractId)
                _editContractUiState.value = UiState.Success(finalContract!!)
            } catch (e: Exception) {
                Log.e(TAG, "onSubmit: error updating contract", e)
                _editContractUiState.value = UiState.Error(e.message ?: "Error updating contract")
            }
        }
    }
}
