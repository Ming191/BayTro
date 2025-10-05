package com.example.baytro.viewModel.Room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.room.Furniture
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.room.Status
import com.example.baytro.utils.AddRoomValidator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditRoomVM (
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room
    private val _editRoomUIState = MutableStateFlow<UiState<Room>>(UiState.Idle)
    val editRoomUIState: StateFlow<UiState<Room>> = _editRoomUIState

    private val _editRoomFormState = MutableStateFlow(EditRoomFormState())
    val editRoomFormState: StateFlow<EditRoomFormState> = _editRoomFormState

    fun loadRoom() {
        viewModelScope.launch {
            try {
                val room = roomRepository.getById(roomId)
                _room.value = room

                room?.let { // formstate copy default value of current room to display
                    _editRoomFormState.value = EditRoomFormState(
                        buildingName = it.buildingId,
                        roomNumber = it.roomNumber,
                        floor = it.floor.toString(),
                        size = it.size.toString(),
                        rentalFee = it.rentalFee.toString(),
                        interior = it.interior
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onBuildingNameChange(buildingName: String) {
        _editRoomFormState.value = _editRoomFormState.value.copy(
            buildingName = buildingName,
            buildingNameError = AddRoomValidator.validateBuildingName(buildingName)
        )
    }

    fun onRoomNumberChange(roomNumber: String) {
        _editRoomFormState.value = _editRoomFormState.value.copy(
            roomNumber = roomNumber,
            roomNumberError = AddRoomValidator.validateRoomNumber(roomNumber)
        )
    }

    fun onFloorChange(floor: String) {
        _editRoomFormState.value = _editRoomFormState.value.copy(
            floor = floor,
            floorError = AddRoomValidator.validateFloor(floor)
        )
    }

    fun onSizeChange(size: String) {
        _editRoomFormState.value = _editRoomFormState.value.copy(
            size = size,
            sizeError = AddRoomValidator.validateSize(size)
        )
    }

    fun onRentalFeeChange(rentalFee: String) {
        _editRoomFormState.value = _editRoomFormState.value.copy(
            rentalFee = rentalFee,
            rentalFeeError = AddRoomValidator.validateRentalFee(rentalFee)
        )
    }

    fun onInteriorChange(interior: Furniture) {
        _editRoomFormState.value = _editRoomFormState.value.copy(
            interior = interior,
            interiorError = AddRoomValidator.validateInterior(interior)
        )
    }

    fun editRoom() {
        val form = _editRoomFormState.value
        val updated = form.copy(
            buildingNameError = AddRoomValidator.validateBuildingName(form.buildingName),
            roomNumberError = AddRoomValidator.validateRoomNumber(form.roomNumber),
            floorError = AddRoomValidator.validateFloor(form.floor),
            sizeError = AddRoomValidator.validateSize(form.size),
            rentalFeeError = AddRoomValidator.validateRentalFee(form.rentalFee),
            interiorError = AddRoomValidator.validateInterior(form.interior)
        )
        _editRoomFormState.value = updated
        if (listOf(
                updated.buildingNameError,
                updated.roomNumberError,
                updated.floorError,
                updated.sizeError,
                updated.rentalFeeError,
                updated.interiorError
            ).any { it != null }
        ) {
            return
        }

        _editRoomUIState.value = UiState.Loading
        try {
            val formState = _editRoomFormState.value
            val updatedRoom = Room(
                id = "",
                buildingId = formState.buildingName,
                floor = formState.floor.toIntOrNull()?:0,
                roomNumber = formState.roomNumber,
                size = formState.size.toIntOrNull()?:0,
                rentalFee = formState.rentalFee.toIntOrNull()?:0,
                status = Status.Available,
                interior = formState.interior,
            )
            viewModelScope.launch {
                roomRepository.update(roomId, updatedRoom)
                _editRoomUIState.value = UiState.Success(updatedRoom)
            }
        }
        catch (e: Exception) {
            _editRoomUIState.value = UiState.Error(e.message ?: "An unknown error occurred")
        }
    }
}