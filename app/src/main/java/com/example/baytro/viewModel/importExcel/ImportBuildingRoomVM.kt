package com.example.baytro.viewModel.importExcel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.BuildingStatus
import com.example.baytro.data.room.Furniture
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.room.Status
import com.example.baytro.utils.BuildingValidator
import com.example.baytro.utils.ExcelImportParser
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportResult(
    val buildingsImported: Int,
    val buildingsUpdated: Int,
    val buildingsFailed: Int,
    val roomsImported: Int,
    val roomsUpdated: Int,
    val roomsFailed: Int
)

class ImportBuildingRoomVM(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
) : ViewModel() {

    private val _importState = MutableStateFlow<UiState<ImportResult>>(UiState.Idle)
    val importState: StateFlow<UiState<ImportResult>> = _importState

    private val _summary = MutableStateFlow<List<String>?>(null)
    val summary: StateFlow<List<String>?> = _summary

    fun startImport(uri: Uri) {
        viewModelScope.launch {
            _importState.value = UiState.Loading
            _summary.value = emptyList()
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user")

                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open file")

                val parsed = withContext(Dispatchers.IO) { ExcelImportParser.parse(input) }
                val logs = mutableListOf<String>()

                var bImported = 0
                var bUpdated = 0
                var bFailed = 0

                // Build a map to locate buildings by (name,address) after creation
                val existingBuildings = buildingRepository.getBuildingsByUserId(currentUser.uid)
                val byKey = existingBuildings.associateBy { keyOf(it.name, it.address) }.toMutableMap()

                // Process buildings
                for (row in parsed.buildings) {
                    val nameErr = BuildingValidator.validateName(row.name)
                    val floorErr = BuildingValidator.validateFloor(row.floors)
                    val addrErr = BuildingValidator.validateAddress(row.address)
                    val billingErr = BuildingValidator.validateBillingDate(row.billingDate)
                    val startErr = BuildingValidator.validatePaymentStart(row.paymentStart)
                    val dueErr = BuildingValidator.validatePaymentDue(row.paymentDue)

                    val firstError = listOf(
                        nameErr, floorErr, addrErr, billingErr, startErr, dueErr
                    ).firstOrNull { it.isError }

                    if (firstError != null) {
                        bFailed++
                        logs.add("Buildings row ${row.rowIndex}: ${firstError.message}")
                        continue
                    }

                    val existing = byKey[keyOf(row.name, row.address)]
                    val buildingStatus = parseBuildingStatus(row.status.ifBlank { "ACTIVE" })
                    val building = Building(
                        id = existing?.id ?: "",
                        name = row.name,
                        floor = row.floors.toInt(),
                        address = row.address,
                        status = buildingStatus,
                        billingDate = row.billingDate.toInt(),
                        paymentStart = row.paymentStart.toInt(),
                        paymentDue = row.paymentDue.toInt(),
                        imageUrls = existing?.imageUrls ?: emptyList(),
                        userId = currentUser.uid,
                        services = existing?.services ?: emptyList(),
                    )

                    if (existing == null) {
                        val id = buildingRepository.add(building)
                        buildingRepository.updateFields(id, mapOf("id" to id))
                        byKey[keyOf(building.name, building.address)] = building.copy(id = id)
                        bImported++
                        logs.add("Building added: ${building.name} - ${building.address}")
                    } else {
                        buildingRepository.update(existing.id, building)
                        bUpdated++
                        logs.add("Building updated: ${building.name} - ${building.address}")
                    }
                }

                // Refresh buildings map after adds/updates
                val allBuildings = buildingRepository.getBuildingsByUserId(currentUser.uid)
                val buildingByKey = allBuildings.associateBy { keyOf(it.name, it.address) }

                var rImported = 0
                var rUpdated = 0
                var rFailed = 0

                // Process rooms
                for (row in parsed.rooms) {
                    // Validate required fields
                    val floorInt = row.floor.toIntOrNull()
                    val sizeInt = row.size.toIntOrNull()
                    val feeInt = row.rentalFee.toIntOrNull()
                    val status = parseStatus(row.status)
                    val interior = parseInterior(row.interior)
                    val errors = mutableListOf<String>()
                    if (row.buildingName.isBlank()) errors.add("Building name is required")
                    if (row.address.isBlank()) errors.add("Address is required")
                    if (row.roomNumber.isBlank()) errors.add("Room number is required")
                    if (floorInt == null || floorInt <= 0) errors.add("Floor must be positive integer")
                    if (sizeInt == null || sizeInt <= 0) errors.add("Size must be positive integer")
                    if (feeInt == null || feeInt < 0) errors.add("Rental fee must be non-negative integer")
                    if (status == null) errors.add("Invalid status (AVAILABLE/OCCUPIED/MAINTENANCE)")
                    if (interior == null) errors.add("Invalid interior (FURNISHED/UNFURNISHED)")

                    val building = buildingByKey[keyOf(row.buildingName, row.address)]
                    if (building == null) {
                        errors.add("Building not found for room")
                    }

                    if (errors.isNotEmpty()) {
                        rFailed++
                        logs.add("Rooms row ${row.rowIndex}: ${errors.first()}")
                        continue
                    }

                    // Determine if room exists: by (buildingId,floor,roomNumber)
                    val existingRooms = roomRepository.getRoomsByBuildingId(building!!.id)
                    val existing = existingRooms.firstOrNull { it.floor == floorInt && it.roomNumber == row.roomNumber }
                    val room = Room(
                        id = existing?.id ?: "",
                        buildingId = building.id,
                        floor = floorInt!!,
                        roomNumber = row.roomNumber,
                        size = sizeInt!!,
                        status = status!!,
                        rentalFee = feeInt!!,
                        interior = interior!!
                    )

                    if (existing == null) {
                        roomRepository.add(room)
                        rImported++
                        logs.add("Room added: ${building.name} - ${row.roomNumber}")
                    } else {
                        roomRepository.update(existing.id, room)
                        rUpdated++
                        logs.add("Room updated: ${building.name} - ${row.roomNumber}")
                    }
                }

                // Include parser-level errors at end
                _summary.value = logs + parsed.errors
                _importState.value = UiState.Success(
                    ImportResult(
                        buildingsImported = bImported,
                        buildingsUpdated = bUpdated,
                        buildingsFailed = bFailed,
                        roomsImported = rImported,
                        roomsUpdated = rUpdated,
                        roomsFailed = rFailed
                    )
                )
            } catch (e: Exception) {
                _importState.value = UiState.Error(e.message ?: "Import failed")
            }
        }
    }

    private fun keyOf(name: String, address: String) = name.trim().lowercase() + "|" + address.trim().lowercase()

    private fun parseStatus(value: String): Status? = when (value.trim().uppercase()) {
        "AVAILABLE" -> Status.AVAILABLE
        "OCCUPIED" -> Status.OCCUPIED
        "MAINTENANCE" -> Status.MAINTENANCE
        else -> null
    }

    private fun parseInterior(value: String): Furniture? = when (value.trim().uppercase()) {
        "FURNISHED" -> Furniture.FURNISHED
        "UNFURNISHED" -> Furniture.UNFURNISHED
        else -> null
    }

    private fun parseBuildingStatus(value: String): BuildingStatus = when (value.trim().uppercase()) {
        "ACTIVE" -> BuildingStatus.ACTIVE
        "INACTIVE" -> BuildingStatus.INACTIVE
        else -> BuildingStatus.ACTIVE // Default to ACTIVE if unknown
    }
}