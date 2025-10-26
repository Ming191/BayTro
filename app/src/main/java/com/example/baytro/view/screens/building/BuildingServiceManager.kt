package com.example.baytro.view.screens.building

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.example.baytro.data.service.Service
import com.example.baytro.view.components.AddServiceBottomSheet
import com.example.baytro.viewModel.building.AddBuildingVM
import kotlinx.coroutines.launch

/**
 * Reusable composable that manages service CRUD operations for building creation.
 * Handles both the bottom sheet for add/edit and the confirmation dialog for delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingServiceManager(
    viewModel: AddBuildingVM,
    sheetState: SheetState,
    showBottomSheet: MutableState<Boolean>,
    showDeleteDialog: MutableState<Boolean>,
    serviceToDelete: MutableState<Service?>,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()

    // Collect states from ViewModel
    val tempServiceName by viewModel.tempServiceName.collectAsState()
    val tempServicePrice by viewModel.tempServicePrice.collectAsState()
    val tempServiceUnit by viewModel.tempServiceUnit.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val isEditingDefaultService by viewModel.isEditingDefaultService.collectAsState()

    // Delete confirmation dialog
    if (showDeleteDialog.value && serviceToDelete.value != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog.value = false
                serviceToDelete.value = null
            },
            title = { Text("Delete Service") },
            text = {
                Text("Are you sure you want to delete \"${serviceToDelete.value?.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        serviceToDelete.value?.let { service ->
                            viewModel.deleteTempService(service)
                            scope.launch {
                                snackbarHostState.showSnackbar("Service deleted!")
                            }
                        }
                        showDeleteDialog.value = false
                        serviceToDelete.value = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog.value = false
                        serviceToDelete.value = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bottom Sheet for adding/editing services
    if (showBottomSheet.value) {
        AddServiceBottomSheet(
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    showBottomSheet.value = false
                    viewModel.clearTempServiceForm()
                }
            },
            name = tempServiceName,
            price = tempServicePrice,
            unit = tempServiceUnit,
            onNameChange = viewModel::updateTempServiceName,
            onPriceChange = viewModel::updateTempServicePrice,
            onUnitSelected = viewModel::updateTempServiceUnit,
            onConfirm = {
                viewModel.addTempService()
                scope.launch {
                    sheetState.hide()
                    showBottomSheet.value = false
                    snackbarHostState.showSnackbar(
                        if (isEditMode) "Service updated!" else "Service added!"
                    )
                }
            },
            isLoading = false,
            isEditMode = isEditMode,
            isDefaultService = isEditingDefaultService
        )
    }
}

