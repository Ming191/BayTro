package com.example.baytro.viewModel.contract

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.building.Building
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddContractVM (
    private val context : Context,
    private val mediaRepo : MediaRepository,
    private val contractRepo : ContractRepository,
    private val roomRepo : RoomRepository,
    private val buildingRepo : BuildingRepository,
    private val auth : AuthRepository
) : ViewModel() {
    companion object {
        private const val TAG = "AddContractVM"
    }

    private val _addContractUiState = MutableStateFlow<UiState<Contract>>(UiState.Idle)
    val addContractUiState : StateFlow<UiState<Contract>> = _addContractUiState

    private val _addContractFormState = MutableStateFlow(AddContractFormState())
    val addContractFormState : StateFlow<AddContractFormState> = _addContractFormState

    init {
        Log.d(TAG, "init: fetching buildings for current user")
        fetchBuildings()
    }

    private fun fetchBuildings() {
        val currentUser = auth.getCurrentUser()
        if (currentUser == null) {
            _addContractUiState.value = UiState.Error("No authenticated user. Please log in again.")
            return
        }
        _addContractUiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val buildings = buildingRepo.getBuildingsByUserId(currentUser.uid)
                _addContractFormState.value = _addContractFormState.value.copy(availableBuildings = buildings)
                if (buildings.isNotEmpty() && _addContractFormState.value.selectedBuilding == null) {
                    onBuildingChange(buildings[0])
                }
                _addContractUiState.value = if (buildings.isEmpty()) {
                    UiState.Error("No buildings found. Please add a building first.")
                } else UiState.Idle
            } catch (e: Exception) {
                _addContractUiState.value = UiState.Error(e.message ?: "Error fetching buildings")
            }
        }
    }

    private fun fetchRooms(buildingId: String) {
        _addContractUiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val rooms = roomRepo.getRoomsByBuildingId(buildingId)
                val contracts = contractRepo.getAll()
                val availableRooms = rooms.filter { room ->
                    contracts.none { contract ->
                        contract.roomId == room.id && contract.status != Status.ENDED
                    }
                }
                _addContractFormState.value = _addContractFormState.value.copy(availableRooms = availableRooms)
                if (availableRooms.isNotEmpty() && _addContractFormState.value.selectedRoom == null) {
                    onRoomChange(availableRooms[0])
                }
                _addContractUiState.value = if (availableRooms.isEmpty()) {
                    UiState.Error("No available rooms found for this building.")
                } else UiState.Idle
            } catch (e: Exception) {
                _addContractFormState.value = _addContractFormState.value.copy(availableRooms = emptyList(), selectedRoom = null)
                _addContractUiState.value = UiState.Error(e.message ?: "Error fetching rooms")
            }
        }
    }


    fun onBuildingChange(building: Building) {
        Log.d(TAG, "onBuildingChange: building=$building")
        _addContractFormState.value =
            _addContractFormState.value.copy(
                selectedBuilding = building,
                buildingIdError = ValidationResult.Success,
                selectedRoom = null)
        if (building.id.isNotBlank()) {
            fetchRooms(building.id)
        } else {
            Log.w(TAG, "onBuildingChange: building has blank id; skipping fetchRooms")
        }
    }

    fun clearError() {
        Log.d(TAG, "clearError: setting UiState to Idle")
        _addContractUiState.value = UiState.Idle
    }

    fun onRoomChange(room: Room) {
        Log.d(TAG, "onRoomChange: room=$room")
        _addContractFormState.value = _addContractFormState.value.copy(selectedRoom = room)
    }

    fun onStartDateChange(startDate: String) {
        Log.d(TAG, "onStartDateChange: startDate=$startDate")
        _addContractFormState.value = _addContractFormState.value.copy(startDate = startDate)
    }

    fun onEndDateChange(endDate: String) {
        Log.d(TAG, "onEndDateChange: endDate=$endDate")
        _addContractFormState.value = _addContractFormState.value.copy(endDate = endDate)
    }

    fun onRentalFeeChange(rentalFee: String) {
        Log.d(TAG, "onRentalFeeChange: rentalFee=$rentalFee")
        _addContractFormState.value = _addContractFormState.value.copy(rentalFee = rentalFee)
    }

    fun onDepositChange(deposit: String) {
        Log.d(TAG, "onDepositChange: deposit=$deposit")
        _addContractFormState.value = _addContractFormState.value.copy(deposit = deposit)
    }

    fun onStatusChange(status: Status) {
        Log.d(TAG, "onStatusChange: status=$status")
        _addContractFormState.value = _addContractFormState.value.copy(status = status)
    }

    fun onPhotosChange(photos: List<Uri>) {
        Log.d(TAG, "onPhotosChange: photosCount=${photos.size}")
        _addContractFormState.value = _addContractFormState.value.copy(selectedPhotos = photos)
    }

    private fun validateInput() : Boolean {
        Log.d(TAG, "validateInput: start")
        val formState = _addContractFormState.value
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
        _addContractFormState.value = formState.copy(
            startDateError = startDateValidator,
            endDateError = endDateValidator,
            rentalFeeError = rentalFeeValidator,
            depositError = depositValidator,
        )
        return isValid
    }

    fun onSubmit() {
        Log.d(TAG, "onSubmit: start")
        if (!validateInput()) {
            Log.w(TAG, "onSubmit: validation failed; aborting submit")
            return
        }
        val formState = _addContractFormState.value
        val currentUser = auth.getCurrentUser()
        if (currentUser == null) {
            Log.w(TAG, "onSubmit: no authenticated user; aborting submit")
            _addContractUiState.value = UiState.Error("No authenticated user. Please log in again.")
            return
        }
        val selectedRoom = formState.selectedRoom
        if (selectedRoom == null) {
            Log.w(TAG, "onSubmit: no room selected; aborting submit")
            _addContractUiState.value = UiState.Error("No room selected. Please select a room.")
            return
        }

        Log.d(TAG, "onSubmit: creating contract with ${formState.selectedPhotos.size} photos")
        viewModelScope.launch {
            _addContractUiState.value = UiState.Loading
            try {
                val newContract = Contract(
                    id = "",
                    tenantIds = emptyList(), //TODO: Set tenant ID when tenant management is implemented
                    roomId = selectedRoom.id,
                    startDate = formState.startDate,
                    endDate = formState.endDate,
                    rentalFee = formState.rentalFee.toInt(),
                    deposit = formState.deposit.toInt(),
                    status = formState.status,
                    photosURL = emptyList(),
                    buildingId = selectedRoom.buildingId,
                    landlordId = currentUser.uid,
                    contractNumber = UUID.randomUUID().toString().take(8),
                    roomNumber = selectedRoom.roomNumber
                )

                Log.d(TAG, "onSubmit: creating contract without photos")
                val contractId = contractRepo.add(newContract)
                Log.d(TAG, "onSubmit: contract created with ID: $contractId")

                val photoUrls = mutableListOf<String>()
                formState.selectedPhotos.forEachIndexed { index, photoUri ->
                    Log.d(TAG, "onSubmit: processing photo $index")
                    val compressedFile = ImageProcessor.compressImageWithCoil(
                        context = context,
                        uri = photoUri,
                        maxWidth = 1080,
                        quality = 85
                    )

                    val photoUrl = mediaRepo.uploadContractPhoto(
                        contractId = contractId,
                        photoIndex = index,
                        imageFile = compressedFile
                    )
                    photoUrls.add(photoUrl)
                    Log.d(TAG, "onSubmit: uploaded photo $index -> $photoUrl")
                }

                if (photoUrls.isNotEmpty()) {
                    Log.d(TAG, "onSubmit: updating contract with ${photoUrls.size} photo URLs")
                    contractRepo.updateFields(contractId, mapOf("photosURL" to photoUrls))
                }

                val finalContract = newContract.copy(id = contractId, photosURL = photoUrls)
                _addContractUiState.value = UiState.Success(finalContract)
            } catch (e: Exception) {
                Log.e(TAG, "onSubmit: error creating contract", e)
                _addContractUiState.value = UiState.Error(e.message ?: "An unknown error occurred while creating contract")
            }
        }
    }
}
