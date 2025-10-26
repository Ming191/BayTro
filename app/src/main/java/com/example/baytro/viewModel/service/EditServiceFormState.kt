package com.example.baytro.viewModel.service

import com.example.baytro.data.Building
import com.example.baytro.data.room.Room
import com.example.baytro.data.service.Metric

data class EditServiceFormState(
    val name: String = "",
    val price: String = "",
    val metrics: Metric = Metric.entries[0],
    val searchText: String = "",
    val isDefault: Boolean = false,

    val availableBuildings: List<Building> = emptyList(),
    val selectedBuilding: Building? = null,

    val availableRooms: List<Room> = emptyList(),
    val selectedRooms: Set<String> = emptySet()
)
