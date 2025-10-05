package com.example.baytro.view.screens.service

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.Building
import com.example.baytro.data.service.Service
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.viewModel.service.AddServiceVM
import com.example.baytro.viewModel.service.AddServiceFormState
import org.koin.compose.viewmodel.koinViewModel
import com.example.baytro.view.screens.UiState

@Composable
fun AddServiceScreen(
    navController: NavHostController,
    viewModel: AddServiceVM = koinViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

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

        UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        UiState.Idle -> Unit
        UiState.Waiting -> TODO()
    }

    AddServiceContent(
        uiState = uiState,
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
    uiState: UiState<Service>,
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
            value = formState.price,
            onValueChange = onPriceChange,
            label = { Text("Unit price") },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownSelectField(
            label = "Metrics",
            options = listOf("Person", "Room", "kWh", "m³"),
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
        Column {
            HorizontalDivider(thickness = 2.dp)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "searchQuery",
                    onValueChange = { },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged {
                        },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    placeholder = {
                        //if (!isSearchFocused) {
                            Text("Search by name or address")
                        //}
                    },
                    label = {
                        //if (!isSearchFocused && searchQuery.isEmpty()) {
                            Text("Search by name or address")
                        //}
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onToggleSelectAll,
                ) {
                    Text("Select all")
                }
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
        SubmitButton(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            isLoading = uiState is UiState.Loading,
            onClick = { onConfirm() }
        )
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
        onConfirm = {},
        uiState = UiState.Idle
    )
}
