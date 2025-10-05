package com.example.baytro.view.screens.contract

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.baytro.data.Building
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.Room
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredDateTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SecondaryButton
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.contract.AddContractFormState
import com.example.baytro.viewModel.contract.AddContractVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddContractScreen(
    viewModel: AddContractVM = koinViewModel(),
    navigateToDetails: (String) -> Unit
) {
    val uiState by viewModel.addContractUiState.collectAsState()
    val formState by viewModel.addContractFormState.collectAsState()

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
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Success -> {
            // Navigate to details screen
            val contractId = (uiState as UiState.Success<Contract>).data.id
            navigateToDetails(contractId)
            viewModel.clearError()
        }
        else -> {}
    }

    AddContractContent(
        formState = formState,
        onBuildingSelected = viewModel::onBuildingChange,
        onRoomSelected = viewModel::onRoomChange,
        onStartDateSelected = viewModel::onStartDateChange,
        onEndDateSelected = viewModel::onEndDateChange,
        onDepositChange = viewModel::onDepositChange,
        onRentalFeeChange = viewModel::onRentalFeeChange,
        onPhotosSelected = viewModel::onPhotosChange,
        onSubmit = viewModel::onSubmit,
        onStatusChange = viewModel::onStatusChange,
        uiState = uiState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContractContent(
    formState: AddContractFormState,
    onBuildingSelected: (Building) -> Unit,
    onRoomSelected: (Room) -> Unit,
    onStartDateSelected: (String) -> Unit = {},
    onEndDateSelected: (String) -> Unit = {},
    onDepositChange: (String) -> Unit = {},
    onRentalFeeChange: (String) -> Unit = {},
    onPhotosSelected: (List<Uri>) -> Unit = {},
    onSubmit: () -> Unit = {},
    onStatusChange: (Status) -> Unit = {},
    uiState: UiState<Contract> = UiState.Idle,
) {
    Scaffold(
        content = { innerPadding ->
            LazyColumn (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
           ) {
                item {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        subhead = "Contract Details"
                    )
                    DropdownSelectField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        label = "Select building",
                        options = formState.availableBuildings,
                        selectedOption = formState.selectedBuilding,
                        onOptionSelected = onBuildingSelected,
                        optionToString = { it.name },
                        enabled = formState.availableBuildings.isNotEmpty()
                    )
                }

                item {
                    DropdownSelectField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        label = "Select room",
                        options = formState.availableRooms,
                        selectedOption = formState.selectedRoom,
                        onOptionSelected = onRoomSelected,
                        optionToString = { it.roomNumber },
                        enabled = formState.availableRooms.isNotEmpty()
                    )
                }

                item {
                    DropdownSelectField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        label = "Status",
                        options = Status.entries,
                        selectedOption = formState.status,
                        onOptionSelected = onStatusChange,
                        optionToString = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        enabled = false
                    )
                }

                item {
                    RequiredDateTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        label = "Start date",
                        selectedDate = formState.startDate,
                        onDateSelected = onStartDateSelected,
                        isError = !formState.startDateError.isSuccess,
                        errorMessage = formState.startDateError.let {
                            if (it is ValidationResult.Error) it.message else null
                        }
                    )
                }

                item {
                    RequiredDateTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        label = "End date",
                        selectedDate = formState.endDate,
                        onDateSelected = onEndDateSelected,
                        isError = !formState.endDateError.isSuccess,
                        errorMessage = (formState.endDateError as? ValidationResult.Error)?.message
                    )
                }

                item {
                    RequiredTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        value = formState.rentalFee,
                        onValueChange = onRentalFeeChange,
                        label = "Rental fee",
                        isError = !formState.rentalFeeError.isSuccess,
                        errorMessage = (formState.rentalFeeError as? ValidationResult.Error)?.message,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }

                item {
                    RequiredTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        value = formState.deposit,
                        onValueChange = onDepositChange,
                        label = "Deposit",
                        isError = !formState.depositError.isSuccess,
                        errorMessage = (formState.depositError as? ValidationResult.Error)?.message,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }

                item {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        subhead = "Service"
                    )
                }

                item {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        subhead = "Contract Photos (Up to 5)"
                    )
                    PhotoCarousel(
                        selectedPhotos = formState.selectedPhotos,
                        onPhotosSelected = onPhotosSelected,
                        maxSelectionCount = 5,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryButton(
                            text = "Back",
                            onClick = { /* TODO: Handle cancel action */ },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        SubmitButton(
                            text = "Save",
                            isLoading = uiState is UiState.Loading,
                            onClick = onSubmit,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
        }
    )
}