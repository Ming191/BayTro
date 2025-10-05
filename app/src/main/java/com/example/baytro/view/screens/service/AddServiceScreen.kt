package com.example.baytro.view.screens.service

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.building.Building
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.viewModel.service.AddServiceVM
import com.example.baytro.viewModel.service.AddServiceFormState
import org.koin.compose.viewmodel.koinViewModel
import com.example.baytro.view.screens.UiState

@Composable
fun AddServiceScreen(
    viewModel: AddServiceVM = koinViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

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

        UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Success<*> -> Unit
        UiState.Idle -> Unit
    }

    AddServiceContent(
        formState = formState,
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onPriceChange = viewModel::onPriceChange,
        onUnitSelected = viewModel::onUnitChange,
        onBuildingSelected = viewModel::onBuildingSelected,
        onToggleRoom = viewModel::onToggleRoom,
        onToggleSelectAll = viewModel::onToggleSelectAll,
        onConfirm = viewModel::onConfirm
    )
}

// --- CONTENT ---
@Composable
fun AddServiceContent(
    formState: AddServiceFormState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUnitSelected: (String) -> Unit,
    onBuildingSelected: (Building) -> Unit,
    onToggleRoom: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onConfirm: () -> Unit,
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
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = formState.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = formState.price,
            onValueChange = onPriceChange,
            label = { Text("Unit price") },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownSelectField(
            label = "Metrics",
            options = listOf("Person", "Room", "Month", "kWh", "m³"),
            selectedOption = formState.unit,
            onOptionSelected = onUnitSelected,
            modifier = Modifier.fillMaxWidth()
        )

        DropdownSelectField(
            label = "Apply to",
            options = formState.availableBuildings,
            selectedOption = formState.selectedBuilding,
            onOptionSelected = onBuildingSelected,
            optionToString = { it.name },
            modifier = Modifier.fillMaxWidth()
        )

        // Search + Select All
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = formState.searchText,
                onValueChange = { /* TODO: implement search in VM */ },
                label = { Text("Search room") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onToggleSelectAll) {
                Text("Select all")
            }
        }

        // Room list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(formState.availableRooms) { room ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(room.roomNumber)
                    Checkbox(
                        checked = formState.selectedRooms.contains(room.id),
                        onCheckedChange = { onToggleRoom(room.id) }
                    )
                }
            }
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirm")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddServiceContentPreview() {
    AddServiceContent(
        formState = AddServiceFormState(
            name = "",
            description = "",
            price = "",
            unit = "",
            availableBuildings = listOf(),
            availableRooms = listOf(),
            selectedRooms = setOf(),
            searchText = ""
        ),
        onNameChange = {},
        onDescriptionChange = {},
        onPriceChange = {},
        onUnitSelected = {},
        onBuildingSelected = {},
        onToggleRoom = {},
        onToggleSelectAll = {},
        onConfirm = {}
    )
}
