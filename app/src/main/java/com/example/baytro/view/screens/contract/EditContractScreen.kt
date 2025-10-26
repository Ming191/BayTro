package com.example.baytro.view.screens.contract

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.service.Service
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.components.AnimatedItem
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredDateTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.contract.EditContractFormState
import com.example.baytro.viewModel.contract.EditContractVM
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditContractScreen(
    viewModel: EditContractVM = koinViewModel(),
    contractId: String,
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(contractId) {
        viewModel.loadContract(contractId)
    }

    val uiState by viewModel.editContractUiState.collectAsState()
    val formState by viewModel.editContractFormState.collectAsState()
    val buildingServices by viewModel.buildingServices.collectAsState()
    val extraServices by viewModel.extraServices.collectAsState()

    when (uiState) {
        is UiState.Error -> {
            val message = (uiState as UiState.Error).message
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Notice") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }
        is UiState.Success -> {
            // Navigate back on success
            LaunchedEffect(Unit) {
                navigateBack()
                viewModel.clearError()
            }
        }
        else -> {}
    }

    EditContractContent(
        formState = formState,
        buildingServices = buildingServices,
        extraServices = extraServices,
        onStartDateSelected = viewModel::onStartDateChange,
        onEndDateSelected = viewModel::onEndDateChange,
        onDepositChange = viewModel::onDepositChange,
        onRentalFeeChange = viewModel::onRentalFeeChange,
        onPhotosSelected = viewModel::onPhotosChange,
        onSubmit = { viewModel.onSubmit(context) },
        uiState = uiState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContractContent(
    formState: EditContractFormState,
    buildingServices: List<Service> = emptyList(),
    extraServices: List<Service> = emptyList(),
    onStartDateSelected: (String) -> Unit = {},
    onEndDateSelected: (String) -> Unit = {},
    onDepositChange: (String) -> Unit = {},
    onRentalFeeChange: (String) -> Unit = {},
    onPhotosSelected: (List<Uri>) -> Unit = {},
    onSubmit: () -> Unit = {},
    uiState: UiState<Contract> = UiState.Idle,
) {
    // Animation states for each section
    var showContractDetails by remember { mutableStateOf(false) }
    var showContractPeriod by remember { mutableStateOf(false) }
    var showFinancialDetails by remember { mutableStateOf(false) }
    var showBuildingServices by remember { mutableStateOf(false) }
    var showExtraServices by remember { mutableStateOf(false) }
    var showPhotos by remember { mutableStateOf(false) }

    // Staggered animation trigger
    LaunchedEffect(Unit) {
        showContractDetails = true
        delay(100)
        showContractPeriod = true
        delay(100)
        showFinancialDetails = true
        delay(100)
        showBuildingServices = true
        delay(100)
        showExtraServices = true
        delay(100)
        showPhotos = true
    }

    Scaffold(
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    SubmitButton(
                        text = "Update Contract",
                        isLoading = uiState is UiState.Loading,
                        onClick = onSubmit,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Contract Details Section
            item {
                AnimatedItem(visible = showContractDetails) {
                    SectionTitle(
                        title = "Contract Details"
                    )
                }
            }

            item {
                AnimatedItem(visible = showContractDetails) {
                    InfoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoRow(
                                icon = Icons.Filled.Business,
                                label = "Building",
                                value = formState.selectedBuilding?.name ?: "N/A"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(
                                icon = Icons.Filled.MeetingRoom,
                                label = "Room Number",
                                value = formState.selectedRoom?.roomNumber ?: "N/A"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(
                                icon = Icons.Filled.Info,
                                label = "Status",
                                value = formState.status.name.lowercase().replaceFirstChar { it.uppercase() }
                            )
                        }
                    }
                }
            }

            // Contract Period Section
            item {
                AnimatedItem(visible = showContractPeriod) {
                    SectionTitle(
                        title = "Contract Period"
                    )
                }
            }

            item {
                AnimatedItem(visible = showContractPeriod) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RequiredDateTextField(
                            modifier = Modifier.fillMaxWidth(),
                            label = "Start date",
                            selectedDate = formState.startDate,
                            onDateSelected = onStartDateSelected,
                            isError = !formState.startDateError.isSuccess,
                            errorMessage = formState.startDateError.let {
                                if (it is ValidationResult.Error) it.message else null
                            }
                        )

                        RequiredDateTextField(
                            modifier = Modifier.fillMaxWidth(),
                            label = "End date",
                            selectedDate = formState.endDate,
                            onDateSelected = onEndDateSelected,
                            isError = !formState.endDateError.isSuccess,
                            errorMessage = (formState.endDateError as? ValidationResult.Error)?.message
                        )
                    }
                }
            }

            // Financial Details Section
            item {
                AnimatedItem(visible = showFinancialDetails) {
                    SectionTitle(
                        title = "Financial Details"
                    )
                }
            }

            item {
                AnimatedItem(visible = showFinancialDetails) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RequiredTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = formState.rentalFee,
                            onValueChange = onRentalFeeChange,
                            label = "Rental fee",
                            isError = !formState.rentalFeeError.isSuccess,
                            errorMessage = (formState.rentalFeeError as? ValidationResult.Error)?.message,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )

                        RequiredTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = formState.deposit,
                            onValueChange = onDepositChange,
                            label = "Deposit",
                            isError = !formState.depositError.isSuccess,
                            errorMessage = (formState.depositError as? ValidationResult.Error)?.message,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // Building Services Section
            item {
                AnimatedItem(visible = showBuildingServices) {
                    SectionTitle(
                        title = "Building Services"
                    )
                }
            }

            item {
                AnimatedItem(visible = showBuildingServices) {
                    Crossfade(
                        targetState = buildingServices.isNotEmpty(),
                        label = "buildingServicesState"
                    ) { hasServices ->
                        if (hasServices) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                buildingServices.forEach { service ->
                                    ServiceCard(
                                        service = service,
                                        onEdit = null,
                                        onDelete = null
                                    )
                                }
                            }
                        } else {
                            EmptyStateCard(
                                icon = Icons.Filled.HomeRepairService,
                                message = "No building services available"
                            )
                        }
                    }
                }
            }

            // Extra Services Section
            item {
                AnimatedItem(visible = showExtraServices) {
                    SectionTitle(
                        title = "Extra Services"
                    )
                }
            }

            item {
                AnimatedItem(visible = showExtraServices) {
                    Crossfade(
                        targetState = extraServices.isNotEmpty(),
                        label = "extraServicesState"
                    ) { hasServices ->
                        if (hasServices) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                extraServices.forEach { service ->
                                    ServiceCard(
                                        service = service,
                                        onEdit = null,
                                        onDelete = null
                                    )
                                }
                            }
                        } else {
                            EmptyStateCard(
                                icon = Icons.Filled.MiscellaneousServices,
                                message = "No extra services in this room"
                            )
                        }
                    }
                }
            }

            // Contract Photos Section
            item {
                AnimatedItem(visible = showPhotos) {
                    SectionTitle(
                        title = "Contract Photos (Up to 5)"
                    )
                }
            }

            item {
                AnimatedItem(visible = showPhotos) {
                    PhotoCarousel(
                        selectedPhotos = formState.selectedPhotos,
                        onPhotosSelected = onPhotosSelected,
                        existingImageUrls = formState.existingPhotosURL,
                        maxSelectionCount = 5
                    )
                }
            }
        }
    }
}