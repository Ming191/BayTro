package com.example.baytro.view.screens.room

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.room.Furniture
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.utils.Utils
import com.example.baytro.viewModel.Room.AddRoomVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddRoomScreen(
    backToRoomListScreen: () -> Unit,
    viewModel: AddRoomVM = koinViewModel(),
) {
    // --- State for each TextField ---
    val roomNumber: (String) -> Unit = viewModel::onRoomNumberChange
    val floor: (String) -> Unit = viewModel::onFloorChange
    val size: (String) -> Unit = viewModel::onSizeChange
    val defaultRentalFee: (String) -> Unit = viewModel::onRentalFeeChange
    val interior: (Furniture) -> Unit = viewModel::onInteriorChange

    val uiState by viewModel.addRoomUIState.collectAsState()
    val formState by viewModel.addRoomFormState.collectAsState()
    val buildingName by viewModel.buildingName.collectAsState()
    val services by viewModel.services.collectAsState()
    val context : Context = LocalContext.current
    Log.d("AddRoomScreen", "services: ${services.size}")

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            Toast.makeText(
                context,
                "Room added successfully!",
                Toast.LENGTH_SHORT
            ).show()
            backToRoomListScreen()
        }
    }

    LazyColumn (
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Building information") }
        item {
            RequiredTextField( // building name is displayed
                value = buildingName,
                onValueChange = {},
                label = "Building Name",
                isError = false,
                errorMessage = null   ,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .alpha(0.3f),

                readOnly = true
            )
        }

        item {
            RequiredTextField(
                value = formState.roomNumber,
                onValueChange = roomNumber,
                label = "Room number",
                isError = formState.roomNumberError != null,
                errorMessage = formState.roomNumberError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = formState.floor,
                onValueChange = floor,
                label = "Floor",
                isError = formState.floorError != null,
                errorMessage = formState.floorError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            RequiredTextField(
                value = formState.size,
                onValueChange = size,
                label = "Size",
                isError = formState.sizeError != null,
                errorMessage = formState.sizeError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            var displayValue by remember { mutableStateOf("") }
            RequiredTextField(
                value = displayValue,
                onValueChange = { newValue ->
                    val numericValue = Utils.parseVND(newValue)
                    displayValue = Utils.formatVND(numericValue)
                    defaultRentalFee(numericValue)
                },
                label = "Default rental fee",
                isError = formState.rentalFeeError != null,
                errorMessage = formState.rentalFeeError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Interior condition")
            ChoiceSelection(
                options = Furniture.entries.toList(),
                selectedOption = formState.interior,
                onOptionSelected = interior,
                isError = formState.interiorError != null,
                errorMessage = formState.interiorError,
            )
        }

        item {
            DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Services")
            if (services.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    services.forEach { service ->
                        ServiceCard(
                            service = service,
                            onEdit = {},
                            onDelete = {}
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No services available")
                }
            }
        }
        item {
            SubmitButton(
                isLoading = uiState is UiState.Loading,
                onClick = {
                    viewModel.addRoom()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}