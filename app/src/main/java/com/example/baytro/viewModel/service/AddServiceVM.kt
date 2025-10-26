package com.example.baytro.viewModel.service

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.service.Metric
import com.example.baytro.data.service.Service
import com.example.baytro.data.service.Status
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddServiceVM(
    private val buildingRepo: BuildingRepository,
    private val roomRepo: RoomRepository,
    private val auth: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "AddServiceVM"
    }
    private val buildingId: String? = savedStateHandle["buildingId"]
    private val roomId: String? = savedStateHandle["roomId"]

    private val isFromAddScreen: Boolean = savedStateHandle["isFromAddScreen"] ?: false

    private val _uiState = MutableStateFlow<UiState<Service>>(UiState.Idle)
    val uiState: StateFlow<UiState<Service>> = _uiState

    private val _formState = MutableStateFlow(AddServiceFormState())
    val formState: StateFlow<AddServiceFormState> = _formState

    init {
        Log.d(TAG, "init: fetching buildings for current user")
        Log.d(TAG, "init: roomID = $roomId, buildingID = $buildingId, isFromAddScreen = $isFromAddScreen" )
        fetchBuildings()
    }

    // --- BUILDING ---
    private fun fetchBuildings() {
        val currentUser = auth.getCurrentUser() ?: return
        viewModelScope.launch {
            try {
                // Auto-select building from nav args if available
                if(!buildingId.isNullOrEmpty()){
                    val selected = buildingRepo.getById(buildingId)
                    if (selected != null) {
                        Log.d(TAG, "Auto-selected building from nav args: ${selected.name}")
                        onBuildingSelected(selected)
                        _uiState.value = UiState.Idle
                        return@launch
                    } else {
                        _uiState.value = UiState.Error("Building not found")
                        return@launch
                    }
                } else if(isFromAddScreen) {
                    _formState.value = _formState.value.copy(availableBuildings = emptyList())
                    _uiState.value = UiState.Idle
                    return@launch
                }
                _uiState.value = UiState.Loading
                val buildings = buildingRepo.getBuildingsByUserId(currentUser.uid)
                _formState.value = _formState.value.copy(availableBuildings = buildings)
                // Auto-select first building if available
                if (buildings.isNotEmpty() && _formState.value.selectedBuilding == null) {
                    onBuildingSelected(buildings[0])
                    Log.d(TAG, "Auto-selected first building: ${buildings[0].name}")
                } else {
                    _uiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error while fetching buildings")
            }
        }
    }

    fun onBuildingSelected(building: Building) {
        Log.d(TAG, "onBuildingSelected: ${building.name} - clearing old rooms")

        _formState.value = _formState.value.copy(
            selectedBuilding = building,
            selectedRooms = emptySet(),
            availableRooms = emptyList() // Clear old rooms immediately
        )
        fetchRooms(building.id)
    }

    // --- ROOM ---
    private fun fetchRooms(buildingId: String) {
        viewModelScope.launch {
            try {
                // Don't set loading state here - it causes dropdown to glitch
                val rooms = roomRepo.getRoomsByBuildingId(buildingId)
                _formState.value = _formState.value.copy(availableRooms = rooms)
                Log.d(TAG, "Fetched ${rooms.size} rooms for building $buildingId")
                if (!roomId.isNullOrEmpty()) {
                    val selected = rooms.find { it.id == roomId } ?: roomRepo.getById(roomId)
                    if (selected != null) {
                        Log.d(TAG, "Auto-selected room from nav args: ${selected.roomNumber}")
                        onToggleRoom(selected.id)
                    }
                }
                // Auto-select all rooms
                if (rooms.isNotEmpty() && roomId.isNullOrEmpty()) {
                    val allRoomIds = rooms.map { it.id }.toSet()
                    _formState.value = _formState.value.copy(selectedRooms = allRoomIds)
                    Log.d(TAG, "Auto-selected all ${rooms.size} rooms")
                }

                // Only set to Idle if we're currently Loading (initial load)
                if (_uiState.value is UiState.Loading) {
                    _uiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching rooms for building $buildingId: ${e.message}")
                // Set empty list instead of showing error - building might have no rooms
                _formState.value = _formState.value.copy(availableRooms = emptyList())

                // Only set to Idle if we're currently Loading (initial load)
                if (_uiState.value is UiState.Loading) {
                    _uiState.value = UiState.Idle
                }
            }
        }
    }

    fun onToggleRoom(roomId: String) {
        val current = _formState.value.selectedRooms.toMutableSet()
        if (current.contains(roomId)) current.remove(roomId) else current.add(roomId)
        _formState.value = _formState.value.copy(selectedRooms = current)
    }

    fun onToggleSelectAll() {
        val allRooms = _formState.value.availableRooms.map { it.id }.toSet()
        _formState.value = if (_formState.value.selectedRooms.size == allRooms.size) {
            _formState.value.copy(selectedRooms = emptySet())
        } else {
            _formState.value.copy(selectedRooms = allRooms)
        }
    }

    fun onNameChange(value: String) {
        _formState.value = _formState.value.copy(name = value, nameError = null)
    }
    fun onPriceChange(value: String) {
        _formState.value = _formState.value.copy(price = value, priceError = null)
    }

    fun onUnitChange(value: Metric) { _formState.value = _formState.value.copy(metrics = value) }

    fun onSearchTextChange(value: String) {
        _formState.value = _formState.value.copy(searchText = value)
    }

    fun validateServiceName(name: String): String? {
        return when {
            name.isBlank() -> "Service name cannot be empty"
            name.length < 3 -> "Service name must be at least 3 characters long"
            name.length > 50 -> "Service name is too long"
            else -> null
        }
    }

    fun validatePrice(price: String): String? {
        return when {
            price.isBlank() -> "Price cannot be empty"
            price.toDoubleOrNull() == null -> "Price must be a valid number"
            price.toDouble() < 0 -> "Price cannot be negative"
            else -> null
        }
    }


    fun onConfirm() {
        viewModelScope.launch {
            val state = _formState.value
            Log.d("onConfirm", "confirm add service")
            Log.d("onConfirm", "state: ${state.name}, ${state.price}, isFromAddScreen: $isFromAddScreen")
            if (state.selectedBuilding == null) {
                if (state.name.trim().isEmpty() || state.price.trim().isEmpty() || !isFromAddScreen) {
                    _uiState.value = UiState.Error("Please fill all required fields")
                    return@launch
                }
            }

            val nameError = validateServiceName(state.name)
            val priceError = validatePrice(state.price)

            _formState.value = state.copy(
                nameError = nameError,
                priceError = priceError
            )

            if (nameError != null || priceError != null) return@launch

            Log.d(TAG, "onConfirm: Service name=${state.name}, Building=${state.selectedBuilding?.name}, Selected rooms count=${state.selectedRooms.size}")
            Log.d(TAG, "onConfirm: Selected room IDs=${state.selectedRooms.joinToString()}")

            _uiState.value = UiState.Loading

            try {
                val newService = Service(
                    name = state.name,
                    price = state.price.toIntOrNull() ?: 0,
                    metric = state.metrics,
                    status = Status.ACTIVE
                )

                if(isFromAddScreen) {
                    _uiState.value = UiState.Success(newService)
                    Log.d("onConfirm","sent service successfully")
                    return@launch
                }

                val building = state.selectedBuilding

                val allRoomsSelected = state.selectedRooms.size == state.availableRooms.size && state.availableRooms.isNotEmpty()

                if (state.selectedRooms.isEmpty() || allRoomsSelected && roomId == null) {
                    val serviceId = buildingRepo.addServiceToBuilding(building?.id.toString(), newService)
                    //updatedServices.add(newService)
                    Log.d(TAG, "Service added to building ${building?.name} in subcollection with id=$serviceId")
                    //buildingRepo.updateFields(building.id, mapOf("services" to updatedServices))

                    if (allRoomsSelected) {
                        Log.d(TAG, "All rooms selected - Service added to building ${building?.name} (apply to all)")
                    } else {
                        Log.d(TAG, "No rooms selected - Service added to building ${building?.name} (apply to all)")
                    }
                    _uiState.value = UiState.Success(newService)
                    return@launch
                } else {
                    var successCount = 0
                    var failCount = 0

                    state.selectedRooms.forEach { roomId ->
                        state.selectedRooms.forEach { roomId ->
                            val room = roomRepo.getById(roomId)
                            if (room != null) {
                                try {
                                    val extraServiceId = roomRepo.addExtraServiceToRoom(roomId, newService)
                                    Log.d(TAG, "Service added to room ${room.roomNumber} with extraServiceId=$extraServiceId")
                                    successCount++
                                } catch (e: Exception) {
                                    failCount++
                                    Log.e(TAG, "Error adding extra service to room ${room.roomNumber}: ${e.message}")
                                }
                            } else {
                                failCount++
                                Log.e(TAG, "Room not found: $roomId")
                            }
                        }
                    }

                    Log.d(TAG, "Service added to partial selection: $successCount successful, $failCount failed out of ${state.selectedRooms.size} rooms")

                    if (successCount > 0) {
                        _uiState.value = UiState.Success(newService)
                    } else {
                        _uiState.value = UiState.Error("Failed to add service to any selected rooms")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding service", e)
                _uiState.value = UiState.Error(e.message ?: "Error while adding service")
            }
        }
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }
}
