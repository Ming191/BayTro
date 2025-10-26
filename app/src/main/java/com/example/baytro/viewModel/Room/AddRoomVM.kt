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
import com.example.baytro.data.service.Metric
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
    // Temporary service form state for bottom sheet
    private val _tempServiceName = MutableStateFlow("")
    val tempServiceName: StateFlow<String> = _tempServiceName

    private val _tempServicePrice = MutableStateFlow("")
    val tempServicePrice: StateFlow<String> = _tempServicePrice

    private val _tempServiceUnit = MutableStateFlow(Metric.ROOM)
    val tempServiceUnit: StateFlow<Metric> = _tempServiceUnit

    // Track which service is being edited (by index)
    private var editingServiceIndex: Int? = null

    // Expose whether we're in edit mode
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    // Expose whether the service being edited is a default service
    private val _isEditingDefaultService = MutableStateFlow(false)
    val isEditingDefaultService: StateFlow<Boolean> = _isEditingDefaultService


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
            rentalFee = cleanInput,
            rentalFeeUI = formattedRentalFee
        )
    }

    fun onInteriorChange(interior: Furniture) {
        _addRoomFormState.value = _addRoomFormState.value.copy(interior = interior)
    }

    fun onExtraServiceChange(service: Service) {
        val updatedServices = _extraServices.value.toMutableList()
        updatedServices.add(service)
        _extraServices.value = updatedServices
        _addRoomFormState.value = _addRoomFormState.value.copy(extraService = updatedServices)
    }

    // Service management methods for bottom sheet
    fun updateTempServiceName(value: String) {
        _tempServiceName.value = value
    }

    fun updateTempServicePrice(value: String) {
        _tempServicePrice.value = value
    }

    fun updateTempServiceUnit(unit: Metric) {
        _tempServiceUnit.value = unit
    }

    fun addTempService() {
        val name = _tempServiceName.value.trim()
        val priceStr = _tempServicePrice.value.trim()
        val unit = _tempServiceUnit.value

        if (name.isBlank() || priceStr.isBlank()) {
            Log.w("AddRoomVM", "Cannot add service: name or price is blank")
            return
        }

        val price = priceStr.toIntOrNull()
        if (price == null || price <= 0) {
            Log.w("AddRoomVM", "Cannot add service: invalid price")
            return
        }

        val updatedServices = _extraServices.value.toMutableList()

        if (editingServiceIndex != null && editingServiceIndex!! < updatedServices.size) {
            // Update existing service
            val oldService = updatedServices[editingServiceIndex!!]
            val updatedService = Service(
                id = oldService.id,
                name = name,
                price = price,
                metric = unit,
                status = oldService.status,
                isDefault = oldService.isDefault
            )
            updatedServices[editingServiceIndex!!] = updatedService
            Log.d("AddRoomVM", "Updated service at index $editingServiceIndex: $updatedService")
        } else {
            // Create new service
            val tempService = Service(
                id = "",
                name = name,
                price = price,
                metric = unit,
                status = com.example.baytro.data.service.Status.ACTIVE,
                isDefault = false
            )
            updatedServices.add(tempService)
            Log.d("AddRoomVM", "Added new service: $tempService")
        }

        _extraServices.value = updatedServices
        _addRoomFormState.value = _addRoomFormState.value.copy(extraService = updatedServices)

        // Clear temp form and editing state
        clearTempServiceForm()
    }

    fun clearTempServiceForm() {
        _tempServiceName.value = ""
        _tempServicePrice.value = ""
        _tempServiceUnit.value = Metric.ROOM
        editingServiceIndex = null
        _isEditMode.value = false
        _isEditingDefaultService.value = false
    }

    fun deleteTempService(service: Service) {
        Log.d("AddRoomVM", "Deleting service: ${service.name}")
        val updatedServices = _extraServices.value.toMutableList()
        updatedServices.remove(service)
        _extraServices.value = updatedServices
        _addRoomFormState.value = _addRoomFormState.value.copy(extraService = updatedServices)
    }

    fun editTempService(service: Service) {
        Log.d("AddRoomVM", "Editing service: ${service.name}")
        // Find the index of the service being edited
        editingServiceIndex = _extraServices.value.indexOf(service)

        // Set edit mode
        _isEditMode.value = true

        // Track if editing a default service
        _isEditingDefaultService.value = service.isDefault

        // Populate the temp form with the service data
        _tempServiceName.value = service.name
        _tempServicePrice.value = service.price.toString()
        _tempServiceUnit.value = service.metric
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
