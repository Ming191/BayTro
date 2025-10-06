package com.example.baytro.viewModel.service

import android.util.Log
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
    private val auth: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AddServiceVM"
    }

    private val _uiState = MutableStateFlow<UiState<Service>>(UiState.Idle)
    val uiState: StateFlow<UiState<Service>> = _uiState

    private val _formState = MutableStateFlow(AddServiceFormState())
    val formState: StateFlow<AddServiceFormState> = _formState

    init {
        Log.d(TAG, "init: fetching buildings for current user")
        fetchBuildings()
    }

    // --- BUILDING ---
    private fun fetchBuildings() {
        val currentUser = auth.getCurrentUser() ?: return
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // Add delay for better UX
                kotlinx.coroutines.delay(300)

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
        _formState.value = _formState.value.copy(
            selectedBuilding = building,
            selectedRooms = emptySet()
        )
        fetchRooms(building.id)
    }

    // --- ROOM ---
    private fun fetchRooms(buildingId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // Add delay for better UX
                kotlinx.coroutines.delay(300)

                val rooms = roomRepo.getRoomsByBuildingId(buildingId)
                _formState.value = _formState.value.copy(availableRooms = rooms)
                Log.d(TAG, "Fetched ${rooms.size} rooms for building $buildingId")

                // Auto-select all rooms
                if (rooms.isNotEmpty()) {
                    val allRoomIds = rooms.map { it.id }.toSet()
                    _formState.value = _formState.value.copy(selectedRooms = allRoomIds)
                    Log.d(TAG, "Auto-selected all ${rooms.size} rooms")
                }

                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching rooms for building $buildingId: ${e.message}")
                // Set empty list instead of showing error - building might have no rooms
                _formState.value = _formState.value.copy(availableRooms = emptyList())
                _uiState.value = UiState.Idle
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

    fun onNameChange(value: String) { _formState.value = _formState.value.copy(name = value) }
    fun onPriceChange(value: String) { _formState.value = _formState.value.copy(price = value) }
    fun onUnitChange(value: Metric) { _formState.value = _formState.value.copy(metrics = value) }

    fun onSearchTextChange(value: String) {
        _formState.value = _formState.value.copy(searchText = value)
    }

    fun onConfirm() {
        viewModelScope.launch {
            val state = _formState.value
            if (state.name.isBlank() || state.price.isBlank() || state.selectedBuilding == null) {
                _uiState.value = UiState.Error("Please fill all required fields")
                return@launch
            }

            Log.d(TAG, "onConfirm: Service name=${state.name}, Building=${state.selectedBuilding.name}, Selected rooms count=${state.selectedRooms.size}")
            Log.d(TAG, "onConfirm: Selected room IDs=${state.selectedRooms.joinToString()}")

            _uiState.value = UiState.Loading

            try {
                val newService = Service(
                    name = state.name,
                    price = state.price,
                    metric = state.metrics,
                    status = Status.ACTIVE
                )

                val building = state.selectedBuilding

                val allRoomsSelected = state.selectedRooms.size == state.availableRooms.size && state.availableRooms.isNotEmpty()

                if (state.selectedRooms.isEmpty() || allRoomsSelected) {
                    val updatedServices = building.services.toMutableList()
                    updatedServices.add(newService)

                    buildingRepo.updateFields(building.id, mapOf("services" to updatedServices))

                    if (allRoomsSelected) {
                        Log.d(TAG, "All rooms selected - Service added to building ${building.name} (apply to all)")
                    } else {
                        Log.d(TAG, "No rooms selected - Service added to building ${building.name} (apply to all)")
                    }
                    _uiState.value = UiState.Success(newService)
                } else {
                    var successCount = 0
                    var failCount = 0

                    state.selectedRooms.forEach { roomId ->
                        val room = roomRepo.getById(roomId)
                        if (room != null) {
                            try {
                                val updatedExtraServices = room.extraService.toMutableList()
                                updatedExtraServices.add(newService)

                                val updatedRoom = room.copy(extraService = updatedExtraServices)
                                val success = roomRepo.update(roomId, updatedRoom)

                                if (success) {
                                    successCount++
                                    Log.d(TAG, "Service added to room ${room.roomNumber}")
                                } else {
                                    failCount++
                                    Log.e(TAG, "Failed to update room ${room.roomNumber}")
                                }
                            } catch (e: Exception) {
                                failCount++
                                Log.e(TAG, "Error updating room ${room.roomNumber}: ${e.message}")
                            }
                        } else {
                            failCount++
                            Log.e(TAG, "Room not found: $roomId")
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
