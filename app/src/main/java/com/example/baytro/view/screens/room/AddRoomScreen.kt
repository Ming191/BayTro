package com.example.baytro.view.screens.room

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.room.Furniture
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.Room.AddRoomVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddRoomScreen(
    navController: NavHostController,
    viewModel: AddRoomVM = koinViewModel(),
    buildingId: String
) {
    Log.d("AddRoomScreen", "buildingIdInAddRoomScreen: $buildingId")
    // --- State for each TextField ---
    val roomNumber: (String) -> Unit = viewModel::onRoomNumberChange
    val floor: (String) -> Unit = viewModel::onFloorChange
    val size: (String) -> Unit = viewModel::onSizeChange
    val defaultRentalFee: (String) -> Unit = viewModel::onRentalFeeChange
    val interior: (Furniture) -> Unit = viewModel::onInteriorChange

    val uiState by viewModel.addRoomUIState.collectAsState()
    val formState by viewModel.addRoomFormState.collectAsState()
    val buildingName by viewModel.buildingName.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            Toast.makeText(
                navController?.context,
                "Room added successfully!",
                Toast.LENGTH_SHORT
            ).show()
            navController?.popBackStack()
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
                    .padding(start = 16.dp, end = 16.dp),
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
                isError = formState.floorError != null,
                errorMessage = formState.floorError,
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
                isError = formState.sizeError != null,
                errorMessage = formState.sizeError,
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
                isError = formState.rentalFeeError != null,
                errorMessage = formState.rentalFeeError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            )
        }

        item {
            DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Interior condition")
            ChoiceSelection(
                options = Furniture.entries.toList().dropLast(1), //hide the last options Unknow
                selectedOption = formState.interior,
                onOptionSelected = interior,
                isError = formState.interiorError != null,
                errorMessage = formState.interiorError,
            )
        }

        item {
            DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Services")
            Card(
                Modifier
                    .width(380.dp)
                    .background(Color.White)
            ) {
//                Row(modifier = Modifier.padding(16.dp)) { // Add padding inside the card
//                    Icon(
//                        Icons.Filled.Bolt,
//                        "electric icon"
//                    )
//                    Text(
//                        text = "Electric", // "Electricity" to match the image
//                        modifier = Modifier.padding(start = 8.dp)
//                    )
//                }
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
