package com.example.baytro.view.screens.building

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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.BuildingStatus
import com.example.baytro.data.service.Service
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.building.EditBuildingVM
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBuildingScreen(
    navController: NavHostController? = null,
    buildingId: String,
    viewModel: EditBuildingVM = koinViewModel()
) {
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uiState by viewModel.editUIState.collectAsState()
    val buildingState by viewModel.building.collectAsState()

    // Bottom sheet for adding services
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showBottomSheetState = remember { mutableStateOf(false) }

    // Delete confirmation dialog
    val showDeleteDialogState = remember { mutableStateOf(false) }
    val serviceToDeleteState = remember { mutableStateOf<Service?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val buildingServices by viewModel.buildingServices.collectAsState()

    LaunchedEffect(buildingId) {
        viewModel.load(buildingId)
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is UiState.Success -> {
                Toast.makeText(context, "Building updated successfully", Toast.LENGTH_SHORT).show()
                navController?.popBackStack()
            }
            is UiState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    if (buildingState != null) {
        if (uiState is UiState.Error) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text((uiState as UiState.Error).message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }

        EditBuildingContent(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            onCancel = {
                if (viewModel.hasUnsavedChanges()) {
                    showUnsavedChangesDialog = true
                } else {
                    navController?.popBackStack()
                }
            },
            onAddService = {
                showBottomSheetState.value = true
            },
            buildingServices = buildingServices,
            onEditService = {
                viewModel.editTempService(it)
                showBottomSheetState.value = true
            },
            onDeleteService = {
                serviceToDeleteState.value = it
                showDeleteDialogState.value = true
            }
        )
        
        // Unsaved changes dialog
        if (showUnsavedChangesDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedChangesDialog = false },
                title = { Text("Unsaved Changes") },
                text = { Text("Are you sure you want to leave? Your changes will not be saved.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUnsavedChangesDialog = false
                            navController?.popBackStack()
                        }
                    ) {
                        Text("Leave")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showUnsavedChangesDialog = false }
                    ) {
                        Text("Stay")
                    }
                }
            )
        }
    }

    // Service management (add/edit/delete) with bottom sheet and dialogs
    EditBuildingServiceManager(
        viewModel = viewModel,
        sheetState = sheetState,
        showBottomSheet = showBottomSheetState,
        showDeleteDialog = showDeleteDialogState,
        serviceToDelete = serviceToDeleteState,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBuildingContent(
    viewModel: EditBuildingVM,
    snackbarHostState: SnackbarHostState,
    onCancel: () -> Unit,
    onAddService: () -> Unit,
    buildingServices: List<Service>,
    onEditService: (Service) -> Unit,
    onDeleteService: (Service) -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val formErrors by viewModel.formErrors.collectAsState()
    val uiState by viewModel.editUIState.collectAsState()

    val nameFocus = remember { FocusRequester() }
    val floorFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val billingFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val dueFocus = remember { FocusRequester() }

    // Animation states for each field
    var sectionTitleVisible by remember { mutableStateOf(false) }
    var nameFieldVisible by remember { mutableStateOf(false) }
    var floorFieldVisible by remember { mutableStateOf(false) }
    var addressFieldVisible by remember { mutableStateOf(false) }
    var statusFieldVisible by remember { mutableStateOf(false) }
    var paymentTitleVisible by remember { mutableStateOf(false) }
    var billingFieldVisible by remember { mutableStateOf(false) }
    var paymentFieldsVisible by remember { mutableStateOf(false) }
    var servicesFieldVisible by remember { mutableStateOf(false) }
    var imagesTitleVisible by remember { mutableStateOf(false) }
    var imagesFieldVisible by remember { mutableStateOf(false) }
    var buttonsVisible by remember { mutableStateOf(false) }

    // Trigger staggered animations when form is loaded
    LaunchedEffect(formState.name) {
        if (formState.name.isNotEmpty()) {
            delay(50)
            sectionTitleVisible = true
            buttonsVisible = true
            delay(80)
            nameFieldVisible = true
            delay(80)
            floorFieldVisible = true
            delay(80)
            addressFieldVisible = true
            delay(80)
            statusFieldVisible = true
            delay(100)
            paymentTitleVisible = true
            delay(80)
            billingFieldVisible = true
            delay(80)
            paymentFieldsVisible = true
            delay(80)
            servicesFieldVisible = true
            delay(100)
            imagesTitleVisible = true
            delay(80)
            imagesFieldVisible = true
            delay(80)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                item {
                    AnimatedVisibility(
                        visible = sectionTitleVisible,
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
                            subhead = "Building Details"
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = nameFieldVisible,
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
                            value = formState.name,
                            onValueChange = { viewModel.updateField("name", it) },
                            label = "Building name",
                            isError = formErrors.nameError != null,
                            errorMessage = formErrors.nameError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { floorFocus.requestFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
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
                            onValueChange = { viewModel.updateField("floor", it) },
                            label = "Floor",
                            isError = formErrors.floorError != null,
                            errorMessage = formErrors.floorError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { addressFocus.requestFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(floorFocus)
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = addressFieldVisible,
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
                            value = formState.address,
                            onValueChange = { viewModel.updateField("address", it) },
                            label = "Address",
                            isError = formErrors.addressError != null,
                            errorMessage = formErrors.addressError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { billingFocus.requestFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(addressFocus)
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = statusFieldVisible,
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
                        DropdownSelectField(
                            label = "Status",
                            options = BuildingStatus.entries.filter { it != BuildingStatus.ARCHIVED },
                            selectedOption = formState.status,
                            onOptionSelected = { viewModel.updateField("status", it.name) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            optionToString = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = paymentTitleVisible,
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
                            subhead = "Payment Schedule"
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = billingFieldVisible,
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
                            value = formState.billingDate,
                            onValueChange = { viewModel.updateField("billingDate", it) },
                            label = "Billing date",
                            isError = formErrors.billingError != null,
                            errorMessage = formErrors.billingError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { startFocus.requestFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(billingFocus)
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = paymentFieldsVisible,
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                RequiredTextField(
                                    value = formState.paymentStart,
                                    onValueChange = { viewModel.updateField("paymentStart", it) },
                                    label = "Payment start",
                                    isError = formErrors.startError != null,
                                    errorMessage = formErrors.startError,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { dueFocus.requestFocus() }
                                    ),
                                    modifier = Modifier.fillMaxWidth().focusRequester(startFocus)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                RequiredTextField(
                                    value = formState.paymentDue,
                                    onValueChange = { viewModel.updateField("paymentDue", it) },
                                    label = "Payment due",
                                    isError = formErrors.dueError != null,
                                    errorMessage = formErrors.dueError,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { dueFocus.freeFocus() }
                                    ),
                                    modifier = Modifier.fillMaxWidth().focusRequester(dueFocus)
                                )
                            }
                        }
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
                            if (buildingServices.isNotEmpty()) {
                                buildingServices.forEach { service ->
                                    ServiceCard(
                                        service = service,
                                        onEdit = onEditService,
                                        onDelete = onDeleteService
                                    )
                                }
                            }

                            // Add Service Button
                            Button(
                                onClick = onAddService,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                                    text = if (buildingServices.isEmpty()) "Add Service" else "Add Another Service",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = imagesTitleVisible,
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
                                .padding(vertical = 8.dp),
                            subhead = "Building Images (Up to 3)"
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = imagesFieldVisible,
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
                        PhotoCarousel(
                            selectedPhotos = formState.selectedImages,
                            onPhotosSelected = { viewModel.updateImages(it) },
                            existingImageUrls = formState.existingImageUrls,
                            onExistingImagesChanged = { viewModel.updateExistingImages(it) },
                            maxSelectionCount = 3,
                            imageWidth = 160.dp,
                            imageHeight = 90.dp,
                            aspectRatioX = 16f,
                            aspectRatioY = 9f,
                            maxResultWidth = 1920,
                            maxResultHeight = 1080
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = buttonsVisible,
                enter = fadeIn()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Text(text = "Back")
                    }
                    SubmitButton(
                        text = "Save",
                        isLoading = uiState is UiState.Loading,
                        onClick = {
                            val (isValid, errorField) = viewModel.validateAll()
                            if (!isValid) {
                                when (errorField) {
                                    "name" -> nameFocus.requestFocus()
                                    "floor" -> floorFocus.requestFocus()
                                    "address" -> addressFocus.requestFocus()
                                    "billingDate" -> billingFocus.requestFocus()
                                    "paymentStart" -> startFocus.requestFocus()
                                    "paymentDue" -> dueFocus.requestFocus()
                                }
                            } else {
                                viewModel.saveBuilding()
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    )
}

