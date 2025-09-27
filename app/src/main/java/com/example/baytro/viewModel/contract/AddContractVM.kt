package com.example.baytro.viewModel.contract

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.Room
import com.example.baytro.data.RoomRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.contract.Status
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddContractVM (
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
            Log.w(TAG, "fetchBuildings: no authenticated user; skipping fetch")
            return
        }
        val userId = currentUser.uid
        Log.d(TAG, "fetchBuildings: start for userId=$userId")
        viewModelScope.launch {
            try {
                val buildings = buildingRepo.getBuildingsByUserId(userId)
                Log.d(TAG, "fetchBuildings: fetched ${buildings.size} buildings")
                _addContractFormState.value = _addContractFormState.value.copy(availableBuildings = buildings,)
                if(buildings.isNotEmpty() && _addContractFormState.value.selectedBuilding == null) {
                    Log.d(TAG, "fetchBuildings: selecting default building ${buildings[0]}")
                    onBuildingChange(buildings[0])
                }
                if(buildings.isEmpty()) {
                    Log.w(TAG, "fetchBuildings: no buildings found; prompting user to add one")
                    _addContractUiState.value = UiState.Error("No buildings found. Please add a building first.")
                } else {
                    _addContractUiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                _addContractUiState.value = UiState.Error(e.message ?: "An unknown error occurred while fetching buildings")
                Log.e(TAG, "Error fetching buildings", e)
            }
        }
    }

    private fun fetchRooms(buildingId: String) {
        Log.d(TAG, "fetchRooms: start for buildingId=$buildingId")
        viewModelScope.launch {
            try {
                val rooms = roomRepo.getRoomsByBuildingId(buildingId)
                Log.d(TAG, "fetchRooms: fetched ${rooms.size} rooms")
                _addContractFormState.value =
                    _addContractFormState.value.copy(availableRooms = rooms)

                if (rooms.isNotEmpty() && _addContractFormState.value.selectedRoom == null) {
                    Log.d(TAG, "fetchRooms: selecting default room ${rooms[0]}")
                    onRoomChange(rooms[0])
                }

                _addContractUiState.value =
                    if (rooms.isEmpty()) {
                        Log.w(TAG, "fetchRooms: no rooms found for building $buildingId; prompting user to add one")
                        UiState.Error("No rooms found for this building. Please add a room first.")
                    } else UiState.Idle
            } catch (e: Exception) {
                _addContractFormState.value =
                    _addContractFormState.value.copy(availableRooms = emptyList(), selectedRoom = null)
                _addContractUiState.value =
                    UiState.Error(e.message ?: "An unknown error occurred while fetching rooms")
                Log.e(TAG, "Error fetching rooms for buildingId=$buildingId", e)
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

    fun onRoomChange(roomNumber: Room) {
        Log.d(TAG, "onRoomChange: room=$roomNumber")
        _addContractFormState.value = _addContractFormState.value.copy(selectedRoom = roomNumber,)
    }

    fun onStartDateChange(startDate: String) {
        Log.d(TAG, "onStartDateChange: startDate=$startDate")
        _addContractFormState.value = _addContractFormState.value.copy(startDate = startDate,)
    }

    fun onEndDateChange(endDate: String) {
        Log.d(TAG, "onEndDateChange: endDate=$endDate")
        _addContractFormState.value = _addContractFormState.value.copy(endDate = endDate,)
    }

    fun onRentalFeeChange(rentalFee: String) {
        Log.d(TAG, "onRentalFeeChange: rentalFee=$rentalFee")
        _addContractFormState.value = _addContractFormState.value.copy(rentalFee = rentalFee,)
    }

    fun onDepositChange(deposit: String) {
        Log.d(TAG, "onDepositChange: deposit=$deposit")
        _addContractFormState.value = _addContractFormState.value.copy(deposit = deposit,)
    }

    fun onStatusChange(status: Status) {
        Log.d(TAG, "onStatusChange: status=$status")
        _addContractFormState.value = _addContractFormState.value.copy(status = status,)
    }

    fun onPhotosURLChange(photosURL: List<String>) {
        Log.d(TAG, "onPhotosURLChange: photosCount=${photosURL.size}")
        _addContractFormState.value = _addContractFormState.value.copy(photosURL = photosURL,)
    }

    private fun validateInput() : Boolean {
        Log.d(TAG, "validateInput: start")
        val formState = _addContractFormState.value
        val startDateValidator = Validator.validateInteger(formState.startDate, "Start Date")
        val endDateValidator = Validator.validateInteger(formState.endDate, "End Date")
        val startDateEndDateValidator = Validator.validateStartEndDate(formState.startDate, formState.endDate)
        val rentalFeeValidator = Validator.validateInteger(formState.rentalFee, "Rental Fee")
        val depositValidator = Validator.validateInteger(formState.deposit, "Deposit")
        val photoValidator = Validator.validatePhotosURL(formState.photosURL)

        val allResults = listOf(
            startDateValidator,
            endDateValidator,
            startDateEndDateValidator,
            rentalFeeValidator,
            depositValidator,
            photoValidator
        )

        val isValid = allResults.all { it == ValidationResult.Success }

        if (!isValid) {
            val errors = buildList {
                if (startDateValidator != ValidationResult.Success) add("startDate=$startDateValidator")
                if (endDateValidator != ValidationResult.Success) add("endDate=$endDateValidator")
                if (startDateEndDateValidator != ValidationResult.Success) add("startEndDate=$startDateEndDateValidator")
                if (rentalFeeValidator != ValidationResult.Success) add("rentalFee=$rentalFeeValidator")
                if (depositValidator != ValidationResult.Success) add("deposit=$depositValidator")
                if (photoValidator != ValidationResult.Success) add("photosURL=$photoValidator")
            }
            Log.w(TAG, "validateInput: validation failed -> ${errors.joinToString(", ")}")
        }

        Log.d(TAG, "validateInput: result isValid=$isValid")
        _addContractFormState.value = formState.copy(
            startDateError = startDateValidator,
            endDateError = endDateValidator,
            rentalFeeError = rentalFeeValidator,
            depositError = depositValidator,
            photosURLError = photoValidator,
            startEndDateError = startDateEndDateValidator,
        )
        return isValid
    }
}