package com.example.baytro.viewModel.Room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.Floor
import com.example.baytro.data.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoomListVM(
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val buildingId: String = checkNotNull(savedStateHandle["buildingName"])

    private val _building = MutableStateFlow<Building?>(null)
    val building: StateFlow<Building?> = _building

    private val _floors = MutableStateFlow<List<Floor>>(emptyList())
    val floors: StateFlow<List<Floor>> = _floors

    fun fetchBuilding() {
        viewModelScope.launch {
            try {
                _building.value = buildingRepository.getById(buildingId)
            } catch (e: Exception) {
                e.printStackTrace()
                _building.value = null
            }
        }
    }

    fun fetchRooms() {
        viewModelScope.launch {
            try {
                val building = _building.value ?: buildingRepository.getById(buildingId)
                _building.value = building

                val rooms = roomRepository.getAll()
                val filteredRooms = rooms.filter { it.buildingName == building?.name }
                val roomsGroupByFloor = filteredRooms.groupBy { it.floor }
                val floorsList = roomsGroupByFloor.map { (floorNumber, rooms) ->
                    Floor(number = floorNumber, rooms = rooms)
                }.sortedBy { it.number }

                _floors.value = floorsList
            } catch (e: Exception) {
                e.printStackTrace()
                _floors.value = emptyList()
            }
        }
    }
}
