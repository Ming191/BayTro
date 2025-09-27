package com.example.baytro.view.screens.contract

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.Building
import com.example.baytro.data.Room
import com.example.baytro.data.contract.Contract
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.contract.AddContractFormState
import com.example.baytro.viewModel.contract.AddContractVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddContractScreen(
    viewModel: AddContractVM = koinViewModel()
) {
    val uiState by viewModel.addContractUiState.collectAsState()
    val formState by viewModel.addContractFormState.collectAsState()

    AddContractContent(
        formState = formState,
        onBuildingSelected = viewModel::onBuildingChange,
        onRoomSelected = viewModel::onRoomChange
    )

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
        is UiState.Success -> Unit
        UiState.Idle -> Unit
    }
}

@Composable
fun AddContractContent(
    formState: AddContractFormState,
    onBuildingSelected: (Building) -> Unit,
    onRoomSelected: (Room) -> Unit
) {
    LazyColumn {
        item {
            DropdownSelectField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = "Select building",
                options = formState.availableBuildings,
                selectedOption = formState.selectedBuilding,
                onOptionSelected = onBuildingSelected,
                optionToString = { it.name }
            )
        }

        item {
            DropdownSelectField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = "Select room",
                options = formState.availableRooms,
                selectedOption = formState.selectedRoom,
                onOptionSelected = onRoomSelected,
                optionToString = { it.roomNumber },
                enabled = formState.availableRooms.isNotEmpty()
            )
            if ( formState.availableRooms.isEmpty() ) {
                Text(
                    text = "No rooms found for this building. Please add a room first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}