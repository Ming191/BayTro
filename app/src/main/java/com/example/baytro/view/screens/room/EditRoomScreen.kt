package com.example.baytro.view.screens.room

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.baytro.data.room.Furniture
import com.example.baytro.data.service.Service
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.GeneralServiceManager
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.Room.EditRoomVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoomScreen(
    onBackClick: () -> Unit,
    viewModel: EditRoomVM = koinViewModel(),
) {
    val room by viewModel.room.collectAsState()
    val uiState by viewModel.editRoomUIState.collectAsState()
    val formState by viewModel.editRoomFormState.collectAsState()
    val extraServices by viewModel.extraServices.collectAsState()
    val context : Context = LocalContext.current
    Log.d("EditRoomScreen", "roomInterior: ${room?.interior}")

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

    val roomNumber: (String) -> Unit = viewModel::onRoomNumberChange
    val floor: (String) -> Unit = viewModel::onFloorChange
    val size: (String) -> Unit = viewModel::onSizeChange
    val defaultRentalFee: (String) -> Unit = viewModel::onRentalFeeChange
    val interior: (Furniture) -> Unit = viewModel::onInteriorChange

    LaunchedEffect(Unit) {
        viewModel.loadRoom()
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            Toast.makeText(
                context,
                "Room edited successfully!",
                Toast.LENGTH_SHORT
            ).show()
            onBackClick()
        }
    }

    LazyColumn (
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Building information") }
        item {
            RequiredTextField(
                value = formState.buildingName,
                onValueChange = {},
                label = "Building name",
                isError = false,
                errorMessage = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .alpha(0.3f),
                readOnly = true
            )
        }

        item {
            RequiredTextField(
                value = formState.roomNumber,
                onValueChange = roomNumber,
                label = "Room number",
                isError = formState.roomNumberError != null,
                errorMessage = formState.roomNumberError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = formState.floor,
                onValueChange = floor,
                label = "Floor",
                isError = formState.floorError != null,
                errorMessage = formState.floorError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = formState.size,
                onValueChange = size,
                label = "Size",
                isError = formState.sizeError != null,
                errorMessage = formState.sizeError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = formState.rentalFeeUI,
                onValueChange = defaultRentalFee,
                label = "Default rental fee",
                isError = formState.rentalFeeError != null,
                errorMessage = formState.rentalFeeError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Interior condition")
            ChoiceSelection(
                options = Furniture.entries.toList(),
                selectedOption = formState.interior,
                onOptionSelected = interior,
                isError = formState.interiorError != null,
                errorMessage = formState.interiorError,
            )
        }

        item {
            DividerWithSubhead(modifier = Modifier.padding(16.dp), subhead = "Extra services")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (extraServices.isNotEmpty()) {
                    extraServices.forEach { service ->
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
                    Text(
                        text = if (extraServices.isEmpty()) "Add Service" else "Add Another Service",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        item {
            SubmitButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                isLoading = uiState is UiState.Loading,
                onClick = {
                    viewModel.editRoom()
                }
            )
        }
    }

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
