package com.example.baytro.viewModel.Room

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.room.Furniture
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.room.Status
import com.example.baytro.data.service.Service
import com.example.baytro.data.service.ServiceRepository
import com.example.baytro.utils.AddRoomValidator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
class AddRoomVM(
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val serviceRepository: ServiceRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val buildingId: String = checkNotNull(savedStateHandle["buildingId"])
    private val _addRoomUIState = MutableStateFlow<UiState<Room>>(UiState.Idle)
    val addRoomUIState: StateFlow<UiState<Room>> = _addRoomUIState

    private val _addRoomFormState = MutableStateFlow(AddRoomFormState())
    val addRoomFormState: StateFlow<AddRoomFormState> = _addRoomFormState
    
    private val _buildingName = MutableStateFlow<String>("")
    val buildingName: StateFlow<String> = _buildingName

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services
    
    init {
        loadBuildingName()
        loadService()
        Log.d("AddRoomVM", "buildingIdInAddRoomVM: $buildingId")
    }
    
    private fun loadBuildingName() {
        viewModelScope.launch {
            try {
                val building = buildingRepository.getById(buildingId)
                _buildingName.value = building?.name ?: "Unknown Building"
            } catch (_: Exception) {
                _buildingName.value = "Unknown Building"
            }
        }
    }

    private fun loadService() {
        viewModelScope.launch {
            try {
                val services = buildingRepository.getServicesByBuildingId(buildingId)
                _services.value = services
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
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
            interiorError = AddRoomValidator.validateInterior(form.interior),

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
                buildingId = buildingId,
                floor = formState.floor.toIntOrNull()?:0,
                roomNumber = formState.roomNumber,
                size = formState.size.toIntOrNull()?:0,
                rentalFee = formState.rentalFee.toIntOrNull()?:0,
                status = Status.Available,
                interior = formState.interior,
                extraService = services.value
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