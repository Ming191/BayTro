package com.example.baytro.view.screens.contract

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import com.example.baytro.data.contract.Status
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredDateTextField
import com.example.baytro.view.components.RequiredTextField
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
        onStartDateSelected = viewModel::onStartDateChange,
        onEndDateSelected = viewModel::onEndDateChange,
        onDepositChange = viewModel::onDepositChange,
        onRentalFeeChange = viewModel::onRentalFeeChange,
        onPhotosSelected = viewModel::onPhotosChange,
        onSubmit = { viewModel.onSubmit(context) },
        onStatusChange = viewModel::onStatusChange,
        uiState = uiState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContractContent(
    formState: EditContractFormState,
    onStartDateSelected: (String) -> Unit = {},
    onEndDateSelected: (String) -> Unit = {},
    onDepositChange: (String) -> Unit = {},
    onRentalFeeChange: (String) -> Unit = {},
    onPhotosSelected: (List<Uri>) -> Unit = {},
    onSubmit: () -> Unit = {},
    onStatusChange: (Status) -> Unit = {},
    uiState: UiState<Contract> = UiState.Idle,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 550)) + slideInVertically(
                    animationSpec = tween(300, delayMillis = 550),
                    initialOffsetY = { it / 3 }
                ),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    SubmitButton(
                        text = "Update",
                        isLoading = uiState is UiState.Loading,
                        onClick = onSubmit,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                            animationSpec = tween(300),
                            initialOffsetY = { -it / 3 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        DividerWithSubhead(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            subhead = "Contract Details"
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 50)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 50),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        DropdownSelectField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            label = "Building",
                            options = formState.selectedBuilding?.let { listOf(it) } ?: emptyList(),
                            selectedOption = formState.selectedBuilding,
                            onOptionSelected = { /* Disabled */ },
                            optionToString = { it.name },
                            enabled = false
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 100),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        DropdownSelectField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            label = "Room",
                            options = formState.selectedRoom?.let { listOf(it) } ?: emptyList(),
                            selectedOption = formState.selectedRoom,
                            onOptionSelected = { /* Disabled */ },
                            optionToString = { it.roomNumber },
                            enabled = false
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 150)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 150),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        DropdownSelectField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            label = "Status",
                            options = Status.entries.filter { it != Status.PENDING },
                            selectedOption = formState.status,
                            onOptionSelected = onStatusChange,
                            optionToString = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                            enabled = true
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 200)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 200),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
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
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 250)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 250),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
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
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 300)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 300),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
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
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 350)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 350),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
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
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 400)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 400),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        DividerWithSubhead(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            subhead = "Service"
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 450)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 450),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        DividerWithSubhead(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            subhead = "Contract Photos (Up to 5)"
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 500)) + slideInVertically(
                            animationSpec = tween(300, delayMillis = 500),
                            initialOffsetY = { it / 4 }
                        ),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        PhotoCarousel(
                            selectedPhotos = formState.selectedPhotos,
                            onPhotosSelected = onPhotosSelected,
                            existingImageUrls = formState.existingPhotosURL,
                            maxSelectionCount = 5,
                        )
                    }
                }
            }
        }
    )
}
