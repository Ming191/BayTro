package com.example.baytro.viewModel.Room

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.room.Furniture
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.room.Status
import com.example.baytro.data.service.Service
import com.example.baytro.utils.AddRoomValidator
import com.example.baytro.utils.Utils.formatCurrency
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
class AddRoomVM(
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val buildingId: String = checkNotNull(savedStateHandle["buildingId"])
    private val _addRoomUIState = MutableStateFlow<UiState<Room>>(UiState.Idle)
    val addRoomUIState: StateFlow<UiState<Room>> = _addRoomUIState

    private val _addRoomFormState = MutableStateFlow(AddRoomFormState())
    val addRoomFormState: StateFlow<AddRoomFormState> = _addRoomFormState

    private val _building = MutableStateFlow<Building?>(null)
    val building: StateFlow<Building?> = _building

    private val _extraServices = MutableStateFlow<List<Service>>(emptyList())
    val extraServices: StateFlow<List<Service>> = _extraServices

    var existingRooms = emptyList<Room>()

    init {
        loadBuilding()
        //loadService()
        viewModelScope.launch {
            existingRooms = roomRepository.getRoomsByBuildingId(buildingId)
        }
    }

    private fun loadBuilding() {
        viewModelScope.launch {
            try {
                val building = buildingRepository.getById(buildingId)
                _building.value = building
            } catch (_: Exception) {
                _building.value = null
            }
        }
    }

//    private fun loadService() {
//        viewModelScope.launch {
//            try {
//                val services = buildingRepository.getServicesByBuildingId(buildingId)
//                _extraServices.value = services
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
    fun onRoomNumberChange(roomNumber: String) {
        _addRoomFormState.value = _addRoomFormState.value.copy(roomNumber = roomNumber)
    }

    fun onFloorChange(floor: String) {
        _addRoomFormState.value = _addRoomFormState.value.copy(floor = floor)
    }

    fun onSizeChange(size: String) {
        _addRoomFormState.value = _addRoomFormState.value.copy(size = size)
    }

    fun onRentalFeeChange(rentalFee: String) {
        val cleanInput = rentalFee.replace("D".toRegex(), "")
        val formattedRentalFee = if (cleanInput.isNotEmpty()) formatCurrency(cleanInput) else ""
        _addRoomFormState.value = _addRoomFormState.value.copy(
            rentalFee = cleanInput,           // để lưu DB
            rentalFeeUI = formattedRentalFee    // để hiển thị
        )
    }

    fun onExtraServiceChange(service: Service) {
        val updatedServices = _extraServices.value.toMutableList()
        updatedServices.add(service)
        _extraServices.value = updatedServices
        _addRoomFormState.value = _addRoomFormState.value.copy(_extraService = updatedServices)
    }

    fun onInteriorChange(interior: Furniture) {
        _addRoomFormState.value = _addRoomFormState.value.copy(interior = interior)
    }
    fun addRoom() {
        val form = _addRoomFormState.value
        val updated = form.copy(
            roomNumberError = AddRoomValidator
                .validateRoomNumber(
                    roomNumber = form.roomNumber,
                    floorNumber = _addRoomFormState.value.floor,
                    existingRooms = existingRooms
                ),
            floorError = AddRoomValidator.validateFloor(form.floor),
            sizeError = AddRoomValidator.validateSize(form.size),
            rentalFeeError = AddRoomValidator.validateRentalFee(form.rentalFee),
            interiorError = AddRoomValidator.validateInterior(form.interior),
        )
        _addRoomFormState.value = updated
        if (listOf(
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
                status = Status.AVAILABLE,
                interior = formState.interior,
                //extraService = _services.value
            )
            viewModelScope.launch {
                val newRoomId = roomRepository.add(newRoom)
                _extraServices.value.forEach { service ->
                    roomRepository.addExtraServiceToRoom(newRoomId, service)
                }
                _addRoomUIState.value = UiState.Success(newRoom)
            }
        }
        catch (e: Exception) {
            _addRoomUIState.value = UiState.Error(e.message ?: "An unknown error occurred")
        }
    }
}