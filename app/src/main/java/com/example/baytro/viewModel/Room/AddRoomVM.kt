package com.example.baytro.viewModel.Room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Room
import com.example.baytro.data.RoomRepository
import com.example.baytro.data.Status
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.AuthUIState
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

    fun onRoomNumberChange(roomNumber: Int) {
        _addRoomFormState.value = addRoomFormState.value.copy(roomNumber = roomNumber)
    }

    fun onFloorChange(floor: Int) {
        _addRoomFormState.value = addRoomFormState.value.copy(floor = floor)
    }

    fun onSizeChange(size: Int) {
        _addRoomFormState.value = addRoomFormState.value.copy(size = size)
    }

    fun onRentalFeeChange(rentalFee: Int) {
        _addRoomFormState.value = addRoomFormState.value.copy(rentalFee = rentalFee)
    }

    fun onInteriorChange(interior: Boolean) {
        _addRoomFormState.value = addRoomFormState.value.copy(interior = interior)
    }

    fun addRoom() {
        _addRoomUIState.value = UiState.Loading
        try {
            val formState = _addRoomFormState.value
            val newRoom = Room(
                id = "",
                buildingName = formState.buildingName,
                floor = formState.floor,
                roomNumber = formState.roomNumber,
                size = formState.size,
                rentalFee = formState.rentalFee,
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