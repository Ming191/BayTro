package com.example.baytro.viewModel.request

import com.example.baytro.data.Building

enum class DateFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR
}

data class RequestListFormState(
    val selectedBuilding: Building? = null,
    val availableBuildings: List<Building> = emptyList(),
    val dateFilter: DateFilter = DateFilter.ALL,
    val selectedDay: Int? = null,
    val selectedMonth: Int? = null,
    val selectedYear: Int? = null,
    val isLandlord: Boolean = false
)
