package com.example.baytro.view.screens.service

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.Building
import com.example.baytro.data.service.Metric
import com.example.baytro.data.service.Service
import com.example.baytro.view.components.CompactSearchBar
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.service.AddServiceFormState
import com.example.baytro.viewModel.service.AddServiceVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddServiceScreen(
    navController: NavHostController,
    viewModel: AddServiceVM = koinViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val loading = uiState is UiState.Loading

    Box(modifier = Modifier.fillMaxSize()) {
        // Crossfade between loading and content
        Crossfade(
            targetState = loading,
            animationSpec = tween(durationMillis = 300),
            label = "loadingCrossfade"
        ) { isLoading ->
            if (isLoading) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                AddServiceContent(
                    uiState = uiState,
                    formState = formState,
                    onNameChange = viewModel::onNameChange,
                    onPriceChange = viewModel::onPriceChange,
                    onUnitSelected = viewModel::onUnitChange,
                    onBuildingSelected = viewModel::onBuildingSelected,
                    onToggleRoom = viewModel::onToggleRoom,
                    onToggleSelectAll = viewModel::onToggleSelectAll,
                    onSearchTextChange = viewModel::onSearchTextChange,
                    onConfirm = viewModel::onConfirm,
                    isLoading = loading
                )
            }
        }

        // Show success message
        when (uiState) {
            is UiState.Success -> {
                Toast.makeText(
                    LocalContext.current, "Thêm dịch vụ thành công!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                viewModel.clearError()
            }
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
            UiState.Waiting -> TODO()
            else -> Unit
        }
    }
}

// --- CONTENT ---
@Composable
fun AddServiceContent(
    uiState: UiState<Service>,
    formState: AddServiceFormState,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUnitSelected: (Metric) -> Unit,
    onBuildingSelected: (Building) -> Unit,
    onToggleRoom: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onSearchTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChange,
            label = { Text("Service name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = formState.price,
            onValueChange = onPriceChange,
            label = { Text("Unit price") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        DropdownSelectField(
            label = "Metrics",
            options = Metric.entries.toList(),
            selectedOption = formState.metrics,
            onOptionSelected = onUnitSelected,
            optionToString = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        DropdownSelectField(
            label = "Apply to",
            options = formState.availableBuildings,
            selectedOption = formState.selectedBuilding,
            onOptionSelected = onBuildingSelected,
            optionToString = { it.name },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && formState.availableBuildings.isNotEmpty()
        )

        if (formState.availableRooms.isNotEmpty()) {
            Column {
                HorizontalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactSearchBar(
                        value = formState.searchText,
                        onValueChange = onSearchTextChange,
                        placeholderText = "Search rooms...",
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Checkbox(
                            checked = formState.selectedRooms.size == formState.availableRooms.size && formState.availableRooms.isNotEmpty(),
                            onCheckedChange = { onToggleSelectAll() },
                            enabled = !isLoading
                        )
                        Text("Select all")
                    }
                }
            }

            // Room list - filtered by search text
            val filteredRooms = formState.availableRooms.filter { room ->
                room.roomNumber.contains(formState.searchText, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRooms) { room ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        onClick = {onToggleRoom(room.id)},
                        enabled = !isLoading
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = room.roomNumber,
                                fontWeight = FontWeight.Bold
                            )
                            Checkbox(
                                checked = formState.selectedRooms.contains(room.id),
                                onCheckedChange = { onToggleRoom(room.id) },
                                enabled = !isLoading
                            )
                        }
                    }
                }
            }
        } else if (formState.selectedBuilding != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "This building has no rooms yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Service will be added to building service list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        } else {
            // Empty space when no building is selected
            Spacer(modifier = Modifier.weight(1f))
        }

        SubmitButton(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            isLoading = uiState is UiState.Loading,
            onClick = { onConfirm() }
        )
    }
}