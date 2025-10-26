package com.example.baytro.view.components // Hoặc package đúng của bạn

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch

/**
 * Bottom sheet để thêm/sửa dịch vụ, đã được tối ưu hóa cho UX
 * với xử lý bàn phím và animation mượt mà.
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
    onConfirm: suspend () -> Unit,
    isLoading: Boolean = false,
    isEditMode: Boolean = false,
    isDefaultService: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var isContentVisible by remember { mutableStateOf(false) }

    var showValidation by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isContentVisible = true
        if (!isDefaultService) {
            focusRequester.requestFocus()
        }
    }

    val isFormValid = name.isNotBlank() && price.isNotBlank() && price.toIntOrNull() != null && price.toIntOrNull()!! >= 0

    val submitAction: () -> Unit = {
        scope.launch {
            showValidation = true
            if (isFormValid) {
                keyboardController?.hide()
                kotlinx.coroutines.delay(100)
                onConfirm()
            }
        }
        Unit
    }

    val dismissAction: () -> Unit = {
        scope.launch {
            keyboardController?.hide()
            kotlinx.coroutines.delay(100) // Let keyboard hide first
            isContentVisible = false
            kotlinx.coroutines.delay(400) // Wait for exit animation to complete
            onDismiss()
        }
        Unit
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isLoading) { // Prevent dismissal while loading
                dismissAction()
            }
        },
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            AnimatedVisibility(
                visible = isContentVisible,
                enter = fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(300, easing = LinearOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(350, easing = LinearOutSlowInEasing)) +
                        slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = tween(250, easing = FastOutLinearInEasing)
                        )
            ) {
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
        }
    }
}