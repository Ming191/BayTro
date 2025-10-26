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
    import com.example.baytro.data.service.Metric
    import com.example.baytro.data.service.Service
    import com.example.baytro.utils.EditRoomValidator
    import com.example.baytro.utils.Utils.formatCurrency
    import com.example.baytro.view.screens.UiState
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch
    import java.util.UUID

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

        private var originalServices: List<Service> = emptyList()

        private val _tempServiceName = MutableStateFlow("")
        val tempServiceName: StateFlow<String> = _tempServiceName

        private val _tempServicePrice = MutableStateFlow("")
        val tempServicePrice: StateFlow<String> = _tempServicePrice

        private val _tempServiceUnit = MutableStateFlow(Metric.ROOM)
        val tempServiceUnit: StateFlow<Metric> = _tempServiceUnit

        private var editingServiceId: String? = null
        private val _isEditMode = MutableStateFlow(false)
        val isEditMode: StateFlow<Boolean> = _isEditMode

        private val _isEditingDefaultService = MutableStateFlow(false)
        val isEditingDefaultService: StateFlow<Boolean> = _isEditingDefaultService

        var existingRooms = emptyList<Room>()

        fun loadRoom() {
            viewModelScope.launch {
                _editRoomUIState.value = UiState.Loading
                try {
                    val room = roomRepository.getById(roomId)
                    _room.value = room

                    if (room == null) {
                        _editRoomUIState.value = UiState.Error("Room not found")
                        return@launch
                    }

                    val building = buildingRepository.getById(room.buildingId)
                    existingRooms = roomRepository.getRoomsByBuildingId(room.buildingId)

                    _editRoomFormState.value = EditRoomFormState(
                        buildingName = building?.name ?: "",
                        roomNumber = room.roomNumber,
                        floor = room.floor.toString(),
                        size = room.size.toString(),
                        rentalFeeUI = formatCurrency(room.rentalFee.toString()),
                        rentalFee = room.rentalFee.toString(),
                        interior = room.interior
                    )

                    val services = roomRepository.getExtraServicesByRoomId(roomId)
                    _extraServices.value = services
                    originalServices = services

                    _editRoomUIState.value = UiState.Idle
                } catch (e: Exception) {
                    _editRoomUIState.value = UiState.Error(e.message ?: "Failed to load room data")
                    e.printStackTrace()
                }
            }
        }

        fun onRoomNumberChange(roomNumber: String) {
            _editRoomFormState.value = _editRoomFormState.value.copy(roomNumber = roomNumber)
        }
        fun onFloorChange(floor: String) {
            _editRoomFormState.value = _editRoomFormState.value.copy(floor = floor)
        }
        fun onSizeChange(size: String) {
            _editRoomFormState.value = _editRoomFormState.value.copy(size = size)
        }
        fun onRentalFeeChange(rentalFee: String) {
            val cleanInput = rentalFee.replace("[^\\d]".toRegex(), "")
            val formattedRentalFee = if (cleanInput.isNotEmpty()) formatCurrency(cleanInput) else ""
            _editRoomFormState.value = _editRoomFormState.value.copy(
                rentalFee = cleanInput,
                rentalFeeUI = formattedRentalFee
            )
        }
        fun onInteriorChange(interior: Furniture) {
            _editRoomFormState.value = _editRoomFormState.value.copy(interior = interior)
        }

        fun updateTempServiceName(value: String) { _tempServiceName.value = value }
        fun updateTempServicePrice(value: String) { _tempServicePrice.value = value }
        fun updateTempServiceUnit(unit: Metric) { _tempServiceUnit.value = unit }

        fun clearTempServiceForm() {
            _tempServiceName.value = ""
            _tempServicePrice.value = ""
            _tempServiceUnit.value = Metric.ROOM
            editingServiceId = null
            _isEditMode.value = false
            _isEditingDefaultService.value = false
        }

        fun editTempService(service: Service) {
            editingServiceId = service.id
            _isEditMode.value = true
            _isEditingDefaultService.value = service.isDefault
            _tempServiceName.value = service.name
            _tempServicePrice.value = service.price.toString()
            _tempServiceUnit.value = service.metric
        }


    fun deleteTempService(service: Service) {
        _extraServices.value = _extraServices.value.filter { it.id != service.id }
        Log.d("EditRoomVM", "Service '${service.name}' marked for deletion from UI list.")
    }

    /**
     * Adds or updates a service in the temporary list.
     * Does not interact with the Repository until saveRoom is called.
     */
    fun addTempService() {
        val name = _tempServiceName.value.trim()
        val priceStr = _tempServicePrice.value.trim()
        val unit = _tempServiceUnit.value

        if (name.isBlank() || priceStr.isBlank() || priceStr.toIntOrNull() == null) {
            Log.w("EditRoomVM", "Invalid service input.")
            return
        }

        if (_isEditMode.value && editingServiceId != null) {
            // Update existing service
            val updatedList = _extraServices.value.map {
                if (it.id == editingServiceId) {
                    it.copy(name = name, price = priceStr.toInt(), metric = unit)
                } else {
                    it
                }
            }
            _extraServices.value = updatedList
        } else {
            // Add new service with temporary ID
            val newService = Service(
                id = "temp_${UUID.randomUUID()}",
                name = name,
                price = priceStr.toInt(),
                metric = unit,
                status = com.example.baytro.data.service.Status.ACTIVE,
                isDefault = false
            )
            _extraServices.value = _extraServices.value + newService
        }
        clearTempServiceForm()
    }

        fun editRoom() {
            val form = _editRoomFormState.value
            val validatedForm = form.copy(
                roomNumberError = EditRoomValidator.validateRoomNumber(form.roomNumber, form.floor, existingRooms, roomId),
                floorError = EditRoomValidator.validateFloor(form.floor),
                sizeError = EditRoomValidator.validateSize(form.size),
                rentalFeeError = EditRoomValidator.validateRentalFee(form.rentalFee),
                interiorError = EditRoomValidator.validateInterior(form.interior)
            )
            _editRoomFormState.value = validatedForm

            if (listOf(validatedForm.roomNumberError, validatedForm.floorError, validatedForm.sizeError, validatedForm.rentalFeeError, validatedForm.interiorError).any { it != null }) {
                return
            }

            viewModelScope.launch {
                _editRoomUIState.value = UiState.Loading
                try {
                    // 3. Chuẩn bị dữ liệu phòng đã cập nhật
                    val updatedRoom = Room(
                        id = roomId,
                        buildingId = _room.value?.buildingId ?: "",
                        floor = validatedForm.floor.toInt(),
                        roomNumber = validatedForm.roomNumber,
                        size = validatedForm.size.toInt(),
                        rentalFee = validatedForm.rentalFee.toInt(),
                        status = _room.value?.status ?: Status.AVAILABLE,
                        interior = validatedForm.interior,
                    )

                    val currentServices = _extraServices.value

                    val servicesToDelete = originalServices.filter { original ->
                        currentServices.none { current -> current.id == original.id }
                    }

                    val servicesToAdd = currentServices.filter { it.id.startsWith("temp_") }

                    val servicesToUpdate = currentServices.filter { current ->
                        !current.id.startsWith("temp_") && originalServices.find { it.id == current.id }?.let { it != current } ?: false
                    }

                    Log.d("EditRoomVM", "Submitting changes...")
                    Log.d("EditRoomVM", "-> Services to DELETE: ${servicesToDelete.size}")
                    Log.d("EditRoomVM", "-> Services to ADD: ${servicesToAdd.size}")
                    Log.d("EditRoomVM", "-> Services to UPDATE: ${servicesToUpdate.size}")

                    roomRepository.updateRoomAndServices(
                        roomId = roomId,
                        updatedRoomData = updatedRoom,
                        servicesToAdd = servicesToAdd,
                        servicesToUpdate = servicesToUpdate,
                        servicesToDelete = servicesToDelete
                    )

                    _editRoomUIState.value = UiState.Success(updatedRoom)

                } catch (e: Exception) {
                    _editRoomUIState.value = UiState.Error(e.message ?: "An unknown error occurred")
                    e.printStackTrace()
                }
            }
        }
    }