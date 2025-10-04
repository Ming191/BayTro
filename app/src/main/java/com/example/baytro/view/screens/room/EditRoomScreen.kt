package com.example.baytro.view.screens.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.Furniture
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.Room.EditRoomVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditRoomScreen(
    navController: NavHostController,
    viewModel: EditRoomVM = koinViewModel(),
) {
    val room by viewModel.room.collectAsState()
    val uiState by viewModel.editRoomUIState.collectAsState()
    val formState by viewModel.editRoomFormState.collectAsState()

    // --- State for each TextField ---
    val buildingName: (String) -> Unit =  viewModel::onBuildingNameChange
    val roomNumber: (String) -> Unit = viewModel::onRoomNumberChange
    val floor: (String) -> Unit = viewModel::onFloorChange
    val size: (String) -> Unit = viewModel::onSizeChange
    val defaultRentalFee: (String) -> Unit = viewModel::onRentalFeeChange
    val interior: (Furniture) -> Unit = viewModel::onInteriorChange

    LaunchedEffect(Unit) {
        viewModel.loadRoom()
    }

    LazyColumn (
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { DividerWithSubhead("Building information") }
        item {
            RequiredTextField(
                value = room?.buildingName ?: "", // Bind to state
                onValueChange = buildingName, // Update state
                label = "Building name",
                isError = false, // You'll likely get this from ViewModel validation
                errorMessage = null, // Also from ViewModel
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                readOnly = true
            )
        }

        item {
            RequiredTextField(
                value = formState.roomNumber,
                onValueChange = roomNumber,
                label = "Room number",
                isError = false,
                errorMessage = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = formState.floor.toString(),
                onValueChange = floor,
                label = "Floor",
                isError = false,
                errorMessage = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = formState.size.toString(),
                onValueChange = size,
                label = "Size",
                isError = false,
                errorMessage = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = formState.rentalFee,
                onValueChange = defaultRentalFee,
                label = "Default rental fee",
                isError = false,
                errorMessage = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            DividerWithSubhead("Interior condition")
            ChoiceSelection(
                options = Furniture.entries.toList().dropLast(1),
                selectedOption = formState.interior,
                onOptionSelected = interior,
                isError = false,
                errorMessage = null
            )
        }

        item {
            DividerWithSubhead("Services ", Modifier.padding(start = 16.dp, end = 16.dp))
            Card(
                Modifier
                    .width(380.dp)
                    .background(Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp)) { // Add padding inside the card
                    Icon(
                        Icons.Filled.Bolt,
                        "electric icon"
                    )
                    Text(
                        text = "Electric", // "Electricity" to match the image
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        item {
            SubmitButton(
                isLoading = uiState is UiState.Loading,
                onClick = {
                    viewModel.editRoom()
                }
            )
        }
    }
}
