package com.example.baytro.viewModel.service

import com.example.baytro.data.building.Building
import com.example.baytro.data.room.Room

data class AddServiceFormState(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val unit: String = "",
    val icon: String = "",
    val searchText: String = "",

    val availableBuildings: List<Building> = emptyList(),
    val selectedBuilding: Building? = null,

    val availableRooms: List<Room> = emptyList(),
    val selectedRooms: Set<String> = emptySet()
)
