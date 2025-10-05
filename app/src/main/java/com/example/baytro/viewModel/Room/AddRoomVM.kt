package com.example.baytro.viewModel.Room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.Furniture
import com.example.baytro.data.Room
import com.example.baytro.data.RoomRepository
import com.example.baytro.data.Status
import com.example.baytro.utils.AddRoomValidator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddRoomVM(
    private val roomRepository: RoomRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    val buildingName: String = checkNotNull(savedStateHandle["buildingName"])
    private val _addRoomUIState = MutableStateFlow<UiState<Room>>(UiState.Idle)
    val addRoomUIState: StateFlow<UiState<Room>> = _addRoomUIState

    private val _addRoomFormState = MutableStateFlow(AddRoomFormState())
    val addRoomFormState: StateFlow<AddRoomFormState> = _addRoomFormState
//    fun onBuildingNameChange(buildingName: String) {
//        _addRoomFormState.value = _addRoomFormState.value.copy(
//            buildingName = buildingName,
//            buildingNameError = AddRoomValidator.validateBuildingName(buildingName)
//        )
//    }

    fun onRoomNumberChange(roomNumber: String) {
        _addRoomFormState.value = _addRoomFormState.value.copy(
            roomNumber = roomNumber,
            roomNumberError = AddRoomValidator.validateRoomNumber(roomNumber)
        )
    }

    fun onFloorChange(floor: String) {
        _addRoomFormState.value = _addRoomFormState.value.copy(
            floor = floor,
            floorError = AddRoomValidator.validateFloor(floor)
        )
    }

    fun onSizeChange(size: String) {
        _addRoomFormState.value = _addRoomFormState.value.copy(
            size = size,
            sizeError = AddRoomValidator.validateSize(size)
        )
    }

    fun onRentalFeeChange(rentalFee: String) {
        _addRoomFormState.value = _addRoomFormState.value.copy(
            rentalFee = rentalFee,
            rentalFeeError = AddRoomValidator.validateRentalFee(rentalFee)
        )
    }

    fun onInteriorChange(interior: Furniture) {
        _addRoomFormState.value = _addRoomFormState.value.copy(
            interior = interior,
            interiorError = AddRoomValidator.validateInterior(interior)
        )
    }
    fun addRoom() {
        val form = _addRoomFormState.value
        val updated = form.copy(
            //buildingNameError = AddRoomValidator.validateBuildingName(form.buildingName),
            roomNumberError = AddRoomValidator.validateRoomNumber(form.roomNumber),
            floorError = AddRoomValidator.validateFloor(form.floor),
            sizeError = AddRoomValidator.validateSize(form.size),
            rentalFeeError = AddRoomValidator.validateRentalFee(form.rentalFee),
            interiorError = AddRoomValidator.validateInterior(form.interior)
        )
        _addRoomFormState.value = updated
        if (listOf(
                //updated.buildingNameError,
                updated.roomNumberError,
                updated.floorError,
                updated.sizeError,
                updated.rentalFeeError,
                updated.interiorError
            ).any { it != null }
        ) {
            return
        }

        _addRoomUIState.value = UiState.Loading
        try {
            val formState = _addRoomFormState.value
            val newRoom = Room(
                id = "",
                buildingName = buildingName,
                floor = formState.floor.toIntOrNull()?:0,
                roomNumber = formState.roomNumber,
                size = formState.size.toIntOrNull()?:0,
                rentalFee = formState.rentalFee.toIntOrNull()?:0,
                status = Status.Available,
                interior = formState.interior,
            )
            viewModelScope.launch {
                roomRepository.add(newRoom)
                _addRoomUIState.value = UiState.Success(newRoom)
            }
        }
        catch (e: Exception) {
            _addRoomUIState.value = UiState.Error(e.message ?: "An unknown error occurred")
        }
    }
}