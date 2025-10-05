package com.example.baytro.view.screens.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.example.baytro.data.service.Service
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.service.ServiceListFormState
import com.example.baytro.viewModel.service.ServiceListVM
import org.koin.compose.viewmodel.koinViewModel
import com.example.baytro.data.room.Room
import androidx.compose.ui.tooling.preview.Preview
import com.example.baytro.data.Building
import kotlin.collections.isNotEmpty
@Composable
fun ServiceListScreen(
    navController: NavHostController,
    viewModel: ServiceListVM = koinViewModel()
) {
    val uiState by viewModel.serviceListUiState.collectAsState()
    val formState by viewModel.serviceListFormState.collectAsState()

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
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Success<*> -> Unit
        UiState.Idle -> Unit
        UiState.Waiting -> TODO()
    }

    ServiceListContent(
        formState = formState,
        onBuildingSelected = viewModel::onBuildingChange,
        onEdit = viewModel::onEditService,
        onDelete = viewModel::onDeleteService,
        navController = navController
    )
}

@Composable
fun ServiceListContent (
    formState: ServiceListFormState,
    onEdit: (Service) -> Unit,
    onDelete: (Service) -> Unit,
    onBuildingSelected: (Building) -> Unit,
    navController: NavHostController
) {
    Scaffold { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DropdownSelectField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            label = "Select building",
                            options = formState.availableBuildings,
                            selectedOption = formState.selectedBuilding,
                            onOptionSelected = onBuildingSelected,
                            optionToString = { it.name },
                            enabled = formState.availableBuildings.isNotEmpty()
                        )
                    }

                    items(formState.availableServices) { service ->
                        ServiceCard(
                            service = service,
                            onEdit = onEdit,
                            onDelete = onDelete
                        )
                    }
                }
            }
            FloatingActionButton(
                onClick = { navController.navigate(Screens.AddService.route) },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // OK
                    .padding(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add service")
            }
        }
    }

}

//@Preview
//@Composable
//fun ServiceListPreview(
//) {
//    val buildings = listOf(
//        Building("1", "Building A", 1,"123 Main St", "active", 1, 2, 3),
//        Building("2", "Building 2", 1,"123 Main St", "active", 1, 2, 3),
//    )
//    val services = listOf(
//        Service("1", "Electricity", "Based on meter reading", "4.000", "kWh", "electricity", "1"),
//        Service("2", "Water", "Monthly water fee", "2.000", "m³", "water", "1"),
//        Service("3", "Internet", "High speed fiber", "200.000", "month", "internet", "1"),
//        Service("4", "Cleaning", "Apartment cleaning service", "50.000", "time", "cleaning", "1"),
//        Service("5", "Gas", "Monthly gas fee", "1.500", "m³", "gas", "1"),
//        Service("6", "Parking", "Monthly parking fee", "100.000", "month", "parking", "2"),
//        Service("7", "Security", "24/7 security service", "150.000", "month", "security","2")
//    )
//    val rooms = listOf(
//        Room("1", "101", "1"),
//        Room("2", "102", "1"),
//        Room("3", "201", "2")
//    )
//    ServiceListContent(
//        formState = ServiceListFormState(
//            availableBuildings = buildings,
//            selectedBuilding = buildings[0],
//            availableRooms = rooms,
//            selectedRoom = rooms[0],
//            availableServices = services,
//            selectedService = services[0]
//        ),
//
//        onBuildingSelected = {},
//        onEdit = {},
//        onDelete = {},
//        navController = NavHostController
//    )
//}