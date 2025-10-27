package com.example.baytro.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.baytro.data.service.Metric

/**
 * Simplified bottom sheet for adding services when creating a building.
 * Only collects service name, price, and unit - no building/room selection needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    name: String,
    price: String,
    unit: Metric,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUnitSelected: (Metric) -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false,
    isEditMode: Boolean = false,
    isDefaultService: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    var showValidation by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        visible = true
        focusRequester.requestFocus()
    }

    val isFormValid = name.isNotBlank() && price.isNotBlank() && price.toIntOrNull() != null && price.toIntOrNull()!! >= 0

    val submitAction = {
        showValidation = true
        if (isFormValid) {
            keyboardController?.hide()
            onConfirm()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            AnimatedItem(
                visible = visible,
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isEditMode) "Edit Service" else "Add Service",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = when {
                                    isDefaultService -> "This is a default service. Name and unit cannot be changed. You can only update the price."
                                    isEditMode -> "Update the service details. Changes will be applied when the building is created."
                                    else -> "This service will be added to the building once it's created."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        RequiredTextField(
                            value = name,
                            onValueChange = onNameChange,
                            label = "Service name",
                            isError = showValidation && name.isBlank(),
                            errorMessage = if (showValidation && name.isBlank()) "Service name is required" else null,
                            modifier = if(isDefaultService) {
                                Modifier.fillMaxWidth().focusRequester(focusRequester).alpha(0.4f)
                            } else {
                                Modifier.fillMaxWidth().focusRequester(focusRequester)
                            },
                            readOnly = isLoading || isDefaultService,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RequiredTextField(
                                value = price,
                                onValueChange = onPriceChange,
                                label = "Unit price",
                                isError = showValidation && (price.isBlank() || price.toIntOrNull() == null || price.toIntOrNull()!! < 0),
                                errorMessage = when {
                                    !showValidation -> null
                                    price.isBlank() -> "Price is required"
                                    price.toIntOrNull() == null || price.toIntOrNull()!! < 0 -> "Invalid price"
                                    else -> null
                                },
                                modifier = Modifier.weight(1f),
                                readOnly = isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { submitAction() }
                                )
                            )

                            DropdownSelectField(
                                label = "Unit",
                                options = Metric.entries.toList(),
                                selectedOption = unit,
                                onOptionSelected = onUnitSelected,
                                optionToString = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading && !isDefaultService
                            )
                        }

                        SubmitButton(
                            text = if (isEditMode) "Update Service" else "Add Service",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            isLoading = isLoading,
                            onClick = submitAction
                        )
                    }
                }
            )
        }
    }
}

