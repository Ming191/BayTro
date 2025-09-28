package com.example.baytro.view.screens.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Card
// import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // Import getValue
import androidx.compose.runtime.mutableStateOf // Import mutableStateOf
import androidx.compose.runtime.remember // Import remember
import androidx.compose.runtime.setValue // Import setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.tooling.preview.Preview // Keep if you use previews elsewhere
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.Room
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.Room.AddRoomFormState
import com.example.baytro.viewModel.Room.AddRoomVM
import org.koin.compose.viewmodel.koinViewModel

enum class Furniture {
    Furnished,
    Unfurnished,
}

@Composable
fun AddRoomScreen(
    navController: NavHostController? = null,
    viewModel: AddRoomVM = koinViewModel(),
) {
    // --- State for each TextField ---
    var buildingName by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var defaultRentalFee by remember { mutableStateOf("") }
    var selectedFurniture by remember { mutableStateOf<Furniture?>(null) }
    val uiState by viewModel.addRoomUIState.collectAsState()
    val formState by viewModel.addRoomFormState.collectAsState()


    LazyColumn (
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { DividerWithSubhead("Building information") }
        item {
            RequiredTextField(
                value = buildingName, // Bind to state
                onValueChange = { buildingName = it }, // Update state
                label = "Building name",
                isError = false, // You'll likely get this from ViewModel validation
                errorMessage = null, // Also from ViewModel
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = roomNumber,
                onValueChange = { roomNumber = it },
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
                value = floor,
                onValueChange = { floor = it },
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
                value = size,
                onValueChange = { size = it },
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
                value = defaultRentalFee,
                onValueChange = { defaultRentalFee = it },
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
                options = Furniture.entries.toList(), // Make sure it's a List
                selectedOption = selectedFurniture,
                onOptionSelected = { selectedFurniture = it },
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
                    viewModel.addRoom()
                }
            )
        }
    }
}
