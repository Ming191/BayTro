    package com.example.baytro.viewModel.Room

    import android.util.Log
    import androidx.lifecycle.SavedStateHandle
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.baytro.data.BuildingRepository
    import com.example.baytro.data.room.Furniture
    import com.example.baytro.data.room.Room
    import com.example.baytro.data.room.RoomRepository
    import com.example.baytro.data.room.Status
    import com.example.baytro.data.service.Service
    import com.example.baytro.utils.EditRoomValidator
    import com.example.baytro.utils.Utils.formatCurrency
    import com.example.baytro.view.screens.UiState
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch

    class EditRoomVM (
        private val roomRepository: RoomRepository,
        private val buildingRepository: BuildingRepository,
        savedStateHandle: SavedStateHandle
    ) : ViewModel() {
        private val roomId: String = checkNotNull(savedStateHandle["roomId"])
        private val _room = MutableStateFlow<Room?>(null)
        val room: StateFlow<Room?> = _room
        private val _editRoomUIState = MutableStateFlow<UiState<Room>>(UiState.Idle)
        val editRoomUIState: StateFlow<UiState<Room>> = _editRoomUIState

        private val _editRoomFormState = MutableStateFlow(EditRoomFormState())
        val editRoomFormState: StateFlow<EditRoomFormState> = _editRoomFormState

        private val _extraServices = MutableStateFlow<List<Service>>(emptyList())
        val extraServices: StateFlow<List<Service>> = _extraServices

        var existingRooms = emptyList<Room>()

        init {
            Log.d("EditRoomVM", "roomId: {$roomId}")
            viewModelScope.launch {
                existingRooms = roomRepository.getRoomsByBuildingId(room.value?.buildingId ?: "")
            }
        }

        fun loadRoom() {
            viewModelScope.launch {
                try {
                    val room = roomRepository.getById(roomId)
                    Log.d("EditRoomVM", "room: {$room}")
                    _room.value = room
                    val building = room?.buildingId?.let { buildingRepository.getById(it) }
                    room?.let { // formstate copy default value of current room to display
                        _editRoomFormState.value = EditRoomFormState(
                            buildingName = building?.name ?: "",
                            roomNumber = it.roomNumber,
                            floor = it.floor.toString(),
                            size = it.size.toString(),
                            rentalFeeUI = formatCurrency(it.rentalFee.toString()),
                            rentalFee = it.rentalFee.toString(),
                            interior = it.interior
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun loadService() {
            viewModelScope.launch {
                try {
                    if (_extraServices.value.isNotEmpty()) {
                        Log.d("EditRoomVM", "Skip loading services — already have data in memory")
                        return@launch
                    }

                    val services = roomRepository.getExtraServicesByRoomId(roomId)
                    if (services.isNotEmpty()) {
                        _extraServices.value = services
                    } else {
                        _extraServices.value = emptyList()
                        Log.d("EditRoomVM", "No extra services found for room with ID: $roomId")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun onBuildingNameChange(buildingName: String) {
            _editRoomFormState.value = _editRoomFormState.value.copy(
                buildingName = buildingName,
                buildingNameError = EditRoomValidator.validateBuildingName(buildingName)
            )
        }

        fun onRoomNumberChange(roomNumber: String) {
            _editRoomFormState.value = _editRoomFormState.value.copy(
                roomNumber = roomNumber,
                //roomNumberError = EditRoomValidator.validateRoomNumber(roomNumber)
            )
        }

        fun onFloorChange(floor: String) {
            _editRoomFormState.value = _editRoomFormState.value.copy(
                floor = floor,
                floorError = EditRoomValidator.validateFloor(floor)
            )
        }

        fun onSizeChange(size: String) {
            _editRoomFormState.value = _editRoomFormState.value.copy(
                size = size,
                sizeError = EditRoomValidator.validateSize(size)
            )
        }

        fun onRentalFeeChange(rentalFee: String) {
            val cleanInput = rentalFee.replace("[^\\d]".toRegex(), "")
            val formattedRentalFee = if (cleanInput.isNotEmpty()) formatCurrency(cleanInput) else ""
            _editRoomFormState.value = _editRoomFormState.value.copy(
                rentalFee = cleanInput,           // để lưu DB
                rentalFeeUI = formattedRentalFee,     // để hiển thị
                rentalFeeError = EditRoomValidator.validateRentalFee(cleanInput)
            )
        }

        fun onInteriorChange(interior: Furniture) {
            _editRoomFormState.value = _editRoomFormState.value.copy(
                interior = interior,
                interiorError = EditRoomValidator.validateInterior(interior)
            )
        }

        fun onExtraServiceChange(service: Service) {
            val updatedServices = _extraServices.value.toMutableList()
            updatedServices.add(service)
            _extraServices.value = updatedServices
            //_editRoomFormState.value = _editRoomFormState.value.copy(extraService = updatedServices)
            Log.d("EditRoomVM", "onExtraServiceChange: ${_extraServices.value}")
        }

        fun deleteService(service: Service) {
            viewModelScope.launch {
                try {
                    val roomId = room.value?.id
                    if (roomId != null) {
                        roomRepository. removeExtraServiceFromRoom(roomId,service.id)
                        Log.d("RoomDetailsVM", "Service deleted successfully.")
                    }
                } catch (e: Exception) {
                    Log.e("RoomDetailsVM", "Error deleting service", e)
                }
            }
        }

        fun editRoom() {
            val form = _editRoomFormState.value
            val updated = form.copy(
                buildingNameError = EditRoomValidator.validateBuildingName(form.buildingName),
                roomNumberError = EditRoomValidator.validateRoomNumber(
                    roomNumber = form.roomNumber,
                    floorNumber = _editRoomFormState.value.floor,
                    existingRooms = existingRooms
                ),
                floorError = EditRoomValidator.validateFloor(form.floor),
                sizeError = EditRoomValidator.validateSize(form.size),
                rentalFeeError = EditRoomValidator.validateRentalFee(form.rentalFee),
                interiorError = EditRoomValidator.validateInterior(form.interior)
            )
            _editRoomFormState.value = updated
            if (listOf(
                    updated.buildingNameError,
                    updated.roomNumberError,
                    updated.floorError,
                    updated.sizeError,
                    updated.rentalFeeError,
                    updated.interiorError
                ).any { it != null }
            ) {
                return
            }

            _editRoomUIState.value = UiState.Loading
            try {
                val formState = _editRoomFormState.value
                val updatedRoom = Room(
                    id = "",
                    buildingId = _room.value?.buildingId ?: "",
                    floor = formState.floor.toIntOrNull()?:0,
                    roomNumber = formState.roomNumber,
                    size = formState.size.toIntOrNull()?:0,
                    rentalFee = formState.rentalFee.toIntOrNull()?:0,
                    status = Status.AVAILABLE,
                    interior = formState.interior,
                )
                viewModelScope.launch {
                    roomRepository.update(roomId, updatedRoom)
                    _extraServices.value.forEach { service ->
                        roomRepository.addExtraServiceToRoom(roomId, service)
                    }
                    _editRoomUIState.value = UiState.Success(updatedRoom)
                }
            }
            catch (e: Exception) {
                _editRoomUIState.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }