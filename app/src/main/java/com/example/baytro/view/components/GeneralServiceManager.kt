package com.example.baytro.view.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.baytro.data.service.Metric
import com.example.baytro.data.service.Service
import kotlinx.coroutines.launch

/**
 * General reusable composable for managing service CRUD operations.
 * Can be used across building and room screens for add/edit functionality.
 * 
 * @param sheetState State of the bottom sheet modal
 * @param showBottomSheet Controls whether bottom sheet is visible
 * @param showDeleteDialog Controls whether delete confirmation dialog is visible
 * @param serviceToDelete Service to be deleted (if any)
 * @param snackbarHostState For showing feedback messages
 * @param tempServiceName Current service name in the form
 * @param tempServicePrice Current service price in the form
 * @param tempServiceUnit Current service unit/metric in the form
 * @param isEditMode Whether the form is in edit mode
 * @param isEditingDefaultService Whether editing a default service
 * @param onNameChange Callback when name changes
 * @param onPriceChange Callback when price changes
 * @param onUnitSelected Callback when unit is selected
 * @param onConfirm Callback when form is confirmed
 * @param onDelete Callback when service is deleted
 * @param onDismiss Callback when bottom sheet is dismissed
 * @param onClearForm Callback to clear the form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralServiceManager(
    sheetState: SheetState,
    showBottomSheet: MutableState<Boolean>,
    showDeleteDialog: MutableState<Boolean>,
    serviceToDelete: MutableState<Service?>,
    snackbarHostState: SnackbarHostState,
    tempServiceName: String,
    tempServicePrice: String,
    tempServiceUnit: Metric,
    isEditMode: Boolean,
    isEditingDefaultService: Boolean,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUnitSelected: (Metric) -> Unit,
    onConfirm: () -> Unit,
    onDelete: (Service) -> Unit,
    onDismiss: () -> Unit,
    onClearForm: () -> Unit
) {
    val scope = rememberCoroutineScope()

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
                            onDelete(service)
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
                    onClearForm()
                }
            },
            name = tempServiceName,
            price = tempServicePrice,
            unit = tempServiceUnit,
            onNameChange = onNameChange,
            onPriceChange = onPriceChange,
            onUnitSelected = onUnitSelected,
            onConfirm = {
                onConfirm()
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

