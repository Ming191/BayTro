package com.example.baytro.viewModel.service

import com.example.baytro.data.Building
import com.example.baytro.data.room.Room
import com.example.baytro.data.service.Service

data class ServiceListFormState (
    val selectedBuilding: Building? = null,
    val selectedRoom: Room? = null,
    val selectedService: Service? = null,
    val availableBuildings: List<Building> = emptyList(),
    val availableRooms: List<Room> = emptyList(),
    val availableServices: List<Service> = emptyList()
)