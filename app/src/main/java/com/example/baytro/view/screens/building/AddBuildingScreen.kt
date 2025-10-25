package com.example.baytro.view.screens.building

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.baytro.data.BuildingStatus
import com.example.baytro.data.service.Service
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.AnimatedItem
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.viewModel.building.AddBuildingVM
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBuildingScreen(
    navController: NavHostController? = null,
    viewModel: AddBuildingVM = koinViewModel()
) {
    val uiState by viewModel.addBuildingUIState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val context = LocalContext.current

    // Bottom sheet for adding services
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showBottomSheetState = remember { mutableStateOf(false) }

    // Delete confirmation dialog
    val showDeleteDialogState = remember { mutableStateOf(false) }
    val serviceToDeleteState = remember { mutableStateOf<Service?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val buildingServices by viewModel.buildingServices.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(navController) {
        val savedStateHandle = navController?.currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<String>("newService")?.observe(lifecycleOwner) { json ->
            val service = Json.decodeFromString<Service>(json)
            Log.d("ServiceListScreen", "Received new service: $service")
            viewModel.onBuildingServicesChange(service)
            savedStateHandle.remove<String>("newService")
        }
    }

    val nameFocus = remember { FocusRequester() }
    val floorFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val billingFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val dueFocus = remember { FocusRequester() }

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(
                    context,
                    "Building added successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                navController?.popBackStack()
            }

            is AuthUIState.Error -> {
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            item {
                AnimatedItem(visible) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        subhead = "Building Details"
                    )
                }
            }
            
            item {
                AnimatedItem(visible) {
                    RequiredTextField(
                        value = formState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = "Building name",
                        isError = formState.nameError,
                        errorMessage = formState.nameErrorMsg,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { floorFocus.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                    )
                }
            }

            item {
                AnimatedItem(visible) {
                    RequiredTextField(
                        value = formState.floor,
                        onValueChange = { viewModel.updateFloor(it) },
                        label = "Floor",
                        isError = formState.floorError,
                        errorMessage = formState.floorErrorMsg,
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
                AnimatedItem(visible) {
                    RequiredTextField(
                        value = formState.address,
                        onValueChange = { viewModel.updateAddress(it) },
                        label = "Address",
                        isError = formState.addressError,
                        errorMessage = formState.addressErrorMsg,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { billingFocus.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(addressFocus)
                    )
                }
            }

            item {
                AnimatedItem(visible) {
                    Column {
                        DropdownSelectField(
                            label = "Status",
                            options = BuildingStatus.entries
                                .filter { it != BuildingStatus.ARCHIVED }
                                .map { it.name },
                            selectedOption = formState.status.name,
                            onOptionSelected = { viewModel.updateStatus(BuildingStatus.valueOf(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            optionToString = { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item {
                AnimatedItem(visible) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 4.dp),
                        subhead = "Payment Schedule"
                    )
                }
            }

            item {
                AnimatedItem(visible) {
                    RequiredTextField(
                        value = formState.billingDate,
                        onValueChange = { viewModel.updateBillingDate(it) },
                        label = "Billing date",
                        isError = formState.billingError,
                        errorMessage = formState.billingErrorMsg,
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
                AnimatedItem(visible) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            RequiredTextField(
                                value = formState.paymentStart,
                                onValueChange = { viewModel.updatePaymentStart(it) },
                                label = "Payment start",
                                isError = formState.startError,
                                errorMessage = formState.startErrorMsg,
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
                                onValueChange = { viewModel.updatePaymentDue(it) },
                                label = "Payment due",
                                isError = formState.dueError,
                                errorMessage = formState.dueErrorMsg,
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
                AnimatedItem(visible) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 4.dp),
                        subhead = "Services"
                    )
                }
            }

            item {
                AnimatedItem(visible) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (buildingServices.isNotEmpty()) {
                            buildingServices.forEach { service ->
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
                                showBottomSheetState.value = true
                            },
                            modifier = Modifier.fillMaxWidth(),
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
                AnimatedItem(visible) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 4.dp),
                        subhead = "Building Images"
                    )
                }
            }
            
            item {
                AnimatedItem(visible) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header with image count
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "${formState.selectedImages.size}/3 images selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        PhotoCarousel(
                            selectedPhotos = formState.selectedImages,
                            onPhotosSelected = { viewModel.updateSelectedImages(it) },
                            maxSelectionCount = 3,
                            imageWidth = 200.dp,
                            imageHeight = 150.dp,
                            aspectRatioX = 4f,
                            aspectRatioY = 3f,
                            maxResultWidth = 1080,
                            maxResultHeight = 1080
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            item {
                AnimatedItem(visible) {
                    SubmitButton(
                        text = "Add Building",
                        isLoading = uiState is AuthUIState.Loading,
                        onClick = { viewModel.validateAndSubmit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeight(50.dp)
                    )
                }
            }
        }
    }
    }

    // Service management (add/edit/delete) with bottom sheet and dialogs
    BuildingServiceManager(
        viewModel = viewModel,
        sheetState = sheetState,
        showBottomSheet = showBottomSheetState,
        showDeleteDialog = showDeleteDialogState,
        serviceToDelete = serviceToDeleteState,
        snackbarHostState = snackbarHostState
    )
}

@Preview
@Composable
fun AddBuildingScreenPreview() {
    AddBuildingScreen()
}