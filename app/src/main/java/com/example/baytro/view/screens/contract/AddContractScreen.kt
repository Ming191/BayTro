package com.example.baytro.view.screens.contract

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.Status
import com.example.baytro.data.service.Service
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.components.AnimatedItem
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredDateTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.contract.AddContractFormState
import com.example.baytro.viewModel.contract.AddContractVM
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview(showBackground = true)
fun AddContractScreenPreview() {
    val sampleBuilding = null
    val sampleRoom = null
    val formState = AddContractFormState(
        availableBuildings = sampleBuilding,
        availableRooms = sampleRoom,
        startDate = "2024-07-01",
        endDate = "2025-06-30",
        rentalFee = "1000",
        deposit = "2000",
        status = Status.PENDING
    )

    AddContractContent(
        formState = formState,
        onStartDateSelected = {},
        onEndDateSelected = {},
        onDepositChange = {},
        onRentalFeeChange = {},
        onPhotosSelected = {},
        onSubmit = {},
        onStatusChange = {}
    )
}
@Composable
fun AddContractScreen(
    viewModel: AddContractVM = koinViewModel(),
    navigateToDetails: (String) -> Unit
) {
    val uiState by viewModel.addContractUiState.collectAsState()
    val formState by viewModel.addContractFormState.collectAsState()
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
            val contractId = (uiState as UiState.Success<Contract>).data.id
            navigateToDetails(contractId)
            viewModel.clearError()
        }
        else -> {}
    }

    AddContractContent(
        formState = formState,
        uiState = uiState,
        buildingServices = buildingServices,
        extraServices = extraServices,
        onStartDateSelected = viewModel::onStartDateChange,
        onEndDateSelected = viewModel::onEndDateChange,
        onDepositChange = viewModel::onDepositChange,
        onRentalFeeChange = viewModel::onRentalFeeChange,
        onPhotosSelected = viewModel::onPhotosChange,
        onSubmit = viewModel::onSubmit,
        onStatusChange = viewModel::onStatusChange,
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContractContent(
    buildingServices: List<Service> = emptyList(),
    extraServices: List<Service> = emptyList(),
    formState: AddContractFormState,
    onStartDateSelected: (String) -> Unit = {},
    onEndDateSelected: (String) -> Unit = {},
    onDepositChange: (String) -> Unit = {},
    onRentalFeeChange: (String) -> Unit = {},
    onPhotosSelected: (List<Uri>) -> Unit = {},
    onSubmit: () -> Unit = {},
    onStatusChange: (Status) -> Unit = {},
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
                        text = "Create Contract",
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
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
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
                                value = formState.availableBuildings?.name ?: "N/A"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            InfoRow(
                                icon = Icons.Filled.MeetingRoom,
                                label = "Room Number",
                                value = formState.availableRooms?.roomNumber ?: "N/A"
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

                    DropdownSelectField(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Status",
                        options = Status.entries,
                        selectedOption = formState.status,
                        onOptionSelected = onStatusChange,
                        optionToString = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        enabled = false
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
                        maxSelectionCount = 5
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String
) {
    DividerWithSubhead(
        subhead = title
    )
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}