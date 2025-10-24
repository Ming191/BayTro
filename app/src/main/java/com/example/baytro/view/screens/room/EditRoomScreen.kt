package com.example.baytro.view.screens.room

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.room.Furniture
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.Room.EditRoomVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditRoomScreen(
    onBackClick: () -> Unit,
    onEditExtraServiceClick: (String, String) -> Unit,
    onDeleteServiceClick: () -> Unit,
    viewModel: EditRoomVM = koinViewModel(),
) {
    val room by viewModel.room.collectAsState()
    val uiState by viewModel.editRoomUIState.collectAsState()
    val formState by viewModel.editRoomFormState.collectAsState()
    val context : Context = LocalContext.current
    Log.d("EditRoomScreen", "roomInterior: ${room?.interior}")
    // --- State for each TextField ---
    val buildingName: (String) -> Unit =  viewModel::onBuildingNameChange
    val roomNumber: (String) -> Unit = viewModel::onRoomNumberChange
    val floor: (String) -> Unit = viewModel::onFloorChange
    val size: (String) -> Unit = viewModel::onSizeChange
    val defaultRentalFee: (String) -> Unit = viewModel::onRentalFeeChange
    val interior: (Furniture) -> Unit = viewModel::onInteriorChange

    LaunchedEffect(uiState) {
        viewModel.loadRoom()
        if (uiState is UiState.Success) {
            Toast.makeText(
                context,
                "Room edited successfully!",
                Toast.LENGTH_SHORT
            ).show()
            onBackClick
        }
    }

    LazyColumn (
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Building information") }
        item {
            RequiredTextField(
                value = formState.buildingName, // Bind to state
                onValueChange = buildingName, // Update state
                label = "Building name",
                isError = false, // You'll likely get this from ViewModel validation
                errorMessage = null, // Also from ViewModel
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
                value = formState.rentalFeeUI,
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
                options = Furniture.entries.toList(),
                selectedOption = formState.interior,
                onOptionSelected = interior,
                isError = formState.interiorError != null,
                errorMessage = formState.interiorError,
            )
        }

        item {
            DividerWithSubhead(modifier = Modifier.padding(start = 16.dp, end = 16.dp), subhead = "Services")
            val services = room?.extraService ?: emptyList()
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
                            onEdit = { onEditExtraServiceClick(room?.id.toString(), service.id) },
                            onDelete = { onDeleteServiceClick() }
                        )
                    }
                }
            } else {
                Text(
                    text = "No services available",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        item {
            SubmitButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                isLoading = uiState is UiState.Loading,
                onClick = {
                    viewModel.editRoom()
                }
            )
        }
    }
}
