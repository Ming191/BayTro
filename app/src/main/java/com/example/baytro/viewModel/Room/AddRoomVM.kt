package com.example.baytro.viewModel.Room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Furniture
import com.example.baytro.data.Room
import com.example.baytro.data.RoomRepository
import com.example.baytro.data.Status
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddRoomVM(
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository
): ViewModel() {
    private val _addRoomUIState =   MutableStateFlow<UiState<Room>>(UiState.Idle)
    val addRoomUIState: StateFlow<UiState<Room>> = _addRoomUIState

    private val _addRoomFormState = MutableStateFlow<AddRoomFormState>(AddRoomFormState())
    val addRoomFormState: StateFlow<AddRoomFormState> = _addRoomFormState

    fun onBuildingNameChange(buildingName: String) {
        _addRoomFormState.value = addRoomFormState.value.copy(buildingName = buildingName)
    }

    fun onRoomNumberChange(roomNumber: String) {
        _addRoomFormState.value = addRoomFormState.value.copy(roomNumber = roomNumber)
    }

    fun onFloorChange(floor: String) {
        _addRoomFormState.value = addRoomFormState.value.copy(floor = floor)
    }

    fun onSizeChange(size: String) {
        _addRoomFormState.value = addRoomFormState.value.copy(size = size)
    }

    fun onRentalFeeChange(rentalFee: String) {
        _addRoomFormState.value = addRoomFormState.value.copy(rentalFee = rentalFee)
    }

    fun onInteriorChange(interior: Furniture) {
        _addRoomFormState.value = addRoomFormState.value.copy(interior = interior)
    }

    fun addRoom() {
        _addRoomUIState.value = UiState.Loading
        try {
            val formState = _addRoomFormState.value
            val newRoom = Room(
                id = "",
                buildingName = formState.buildingName,
                floor = formState.floor.toIntOrNull()?:0,
                roomNumber = formState.roomNumber.toIntOrNull()?:0,
                size = formState.size.toIntOrNull()?:0,
                rentalFee = formState.rentalFee.toIntOrNull()?:0,
                status = Status.AVAILABLE,
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