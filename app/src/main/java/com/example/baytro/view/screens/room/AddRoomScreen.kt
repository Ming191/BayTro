package com.example.baytro.view.screens.room

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.baytro.data.room.Furniture
import com.example.baytro.data.service.Service
import com.example.baytro.utils.Utils
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.GeneralServiceManager
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.Room.AddRoomVM
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoomScreen(
    getNewExtraService: (LifecycleOwner, (Service) -> Unit) -> Unit,
    backToRoomListScreen: () -> Unit,
    viewModel: AddRoomVM = koinViewModel()
) {
    val uiState by viewModel.addRoomUIState.collectAsState()
    val formState by viewModel.addRoomFormState.collectAsState()
    val building by viewModel.building.collectAsState()
    val services by viewModel.extraServices.collectAsState()
    val context: Context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Service manager state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showBottomSheetState = remember { mutableStateOf(false) }
    val showDeleteDialogState = remember { mutableStateOf(false) }
    val serviceToDeleteState = remember { mutableStateOf<Service?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tempServiceName by viewModel.tempServiceName.collectAsState()
    val tempServicePrice by viewModel.tempServicePrice.collectAsState()
    val tempServiceUnit by viewModel.tempServiceUnit.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val isEditingDefaultService by viewModel.isEditingDefaultService.collectAsState()

    var displayRentalFee by remember { mutableStateOf("") }

    // Focus requesters
    val roomNumberFocus = remember { FocusRequester() }
    val floorFocus = remember { FocusRequester() }
    val sizeFocus = remember { FocusRequester() }
    val rentalFeeFocus = remember { FocusRequester() }

    // Animation states
    var buildingInfoVisible by remember { mutableStateOf(false) }
    var buildingFieldVisible by remember { mutableStateOf(false) }
    var roomDetailsTitleVisible by remember { mutableStateOf(false) }
    var roomNumberFieldVisible by remember { mutableStateOf(false) }
    var floorFieldVisible by remember { mutableStateOf(false) }
    var sizeFieldVisible by remember { mutableStateOf(false) }
    var rentalFeeFieldVisible by remember { mutableStateOf(false) }
    var interiorTitleVisible by remember { mutableStateOf(false) }
    var interiorFieldVisible by remember { mutableStateOf(false) }
    var servicesTitleVisible by remember { mutableStateOf(false) }
    var servicesFieldVisible by remember { mutableStateOf(false) }
    var buttonsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        getNewExtraService(lifecycleOwner) { service ->
            viewModel.onExtraServiceChange(service)
            Log.d("AddRoomScreen", "Received new service: $service")
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            Toast.makeText(
                context,
                "Room added successfully!",
                Toast.LENGTH_SHORT
            ).show()
            backToRoomListScreen()
        }
    }

    // Trigger animations when building is loaded
    LaunchedEffect(building) {
        if (building != null) {
            delay(50)
            buildingInfoVisible = true
            buttonsVisible = true
            delay(80)
            buildingFieldVisible = true
            delay(80)
            roomDetailsTitleVisible = true
            delay(80)
            roomNumberFieldVisible = true
            delay(80)
            floorFieldVisible = true
            delay(80)
            sizeFieldVisible = true
            delay(80)
            rentalFeeFieldVisible = true
            delay(100)
            interiorTitleVisible = true
            delay(80)
            interiorFieldVisible = true
            delay(100)
            servicesTitleVisible = true
            delay(80)
            servicesFieldVisible = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = buttonsVisible,
                enter = fadeIn()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = backToRoomListScreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    SubmitButton(
                        text = "Create Room",
                        isLoading = uiState is UiState.Loading,
                        onClick = { viewModel.addRoom() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            // Building Information Section
            item {
                AnimatedVisibility(
                    visible = buildingInfoVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        subhead = "Building Information"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = buildingFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    RequiredTextField(
                        value = building?.name.toString(),
                        onValueChange = {},
                        label = "Building Name",
                        isError = false,
                        errorMessage = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(0.5f),
                        readOnly = true
                    )
                }
            }

            // Room Details Section
            item {
                AnimatedVisibility(
                    visible = roomDetailsTitleVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        subhead = "Room Details"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = roomNumberFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    RequiredTextField(
                        value = formState.roomNumber,
                        onValueChange = viewModel::onRoomNumberChange,
                        label = "Room Number",
                        isError = formState.roomNumberError != null,
                        errorMessage = formState.roomNumberError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { floorFocus.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(roomNumberFocus)
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = floorFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    RequiredTextField(
                        value = formState.floor,
                        onValueChange = viewModel::onFloorChange,
                        label = "Floor",
                        isError = formState.floorError != null,
                        errorMessage = formState.floorError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { sizeFocus.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(floorFocus)
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = sizeFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    RequiredTextField(
                        value = formState.size,
                        onValueChange = viewModel::onSizeChange,
                        label = "Size (m²)",
                        isError = formState.sizeError != null,
                        errorMessage = formState.sizeError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { rentalFeeFocus.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(sizeFocus)
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = rentalFeeFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    RequiredTextField(
                        value = displayRentalFee,
                        onValueChange = { newValue ->
                            val numericValue = Utils.parseVND(newValue)
                            displayRentalFee = Utils.formatVND(numericValue)
                            viewModel.onRentalFeeChange(numericValue)
                        },
                        label = "Default Rental Fee",
                        isError = formState.rentalFeeError != null,
                        errorMessage = formState.rentalFeeError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { rentalFeeFocus.freeFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(rentalFeeFocus)
                    )
                }
            }

            // Interior Condition Section
            item {
                AnimatedVisibility(
                    visible = interiorTitleVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 4.dp),
                        subhead = "Interior Condition"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = interiorFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    ChoiceSelection(
                        options = Furniture.entries.toList(),
                        selectedOption = formState.interior,
                        onOptionSelected = viewModel::onInteriorChange,
                        isError = formState.interiorError != null,
                        errorMessage = formState.interiorError,
                    )
                }
            }

            // Services Section
            item {
                AnimatedVisibility(
                    visible = servicesTitleVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 4.dp),
                        subhead = "Services"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = servicesFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (services.isNotEmpty()) {
                            services.forEach { service ->
                                ServiceCard(
                                    service = service,
                                    onEdit = {
                                        viewModel.editTempService(it)
                                        showBottomSheetState.value = true
                                    },
                                    onDelete = {
                                        serviceToDeleteState.value = it
                                        showDeleteDialogState.value = true
                                    }
                                )
                            }
                        }

                        // Add Service Button
                        Button(
                            onClick = {
                                viewModel.clearTempServiceForm()
                                showBottomSheetState.value = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add service",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (services.isEmpty()) "Add Service" else "Add Another Service",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }

    // General service manager for add/edit/delete operations
    GeneralServiceManager(
        sheetState = sheetState,
        showBottomSheet = showBottomSheetState,
        showDeleteDialog = showDeleteDialogState,
        serviceToDelete = serviceToDeleteState,
        snackbarHostState = snackbarHostState,
        tempServiceName = tempServiceName,
        tempServicePrice = tempServicePrice,
        tempServiceUnit = tempServiceUnit,
        isEditMode = isEditMode,
        isEditingDefaultService = isEditingDefaultService,
        onNameChange = viewModel::updateTempServiceName,
        onPriceChange = viewModel::updateTempServicePrice,
        onUnitSelected = viewModel::updateTempServiceUnit,
        onConfirm = viewModel::addTempService,
        onDelete = viewModel::deleteTempService,
        onDismiss = {},
        onClearForm = viewModel::clearTempServiceForm
    )
}