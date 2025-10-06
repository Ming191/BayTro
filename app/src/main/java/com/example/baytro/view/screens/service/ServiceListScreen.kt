package com.example.baytro.view.screens.service

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.Building
import com.example.baytro.data.service.Service
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.service.ServiceListFormState
import com.example.baytro.viewModel.service.ServiceListVM
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ServiceListScreen(
    navController: NavHostController,
    viewModel: ServiceListVM = koinViewModel()
) {
    val uiState by viewModel.serviceListUiState.collectAsState()
    val formState by viewModel.serviceListFormState.collectAsState()

    var indicatorVisible by remember { mutableStateOf(true) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Loading) {
            indicatorVisible = true
            contentVisible = false
        } else {
            indicatorVisible = false
        }
    }

    LaunchedEffect(indicatorVisible) {
        if (!indicatorVisible) {
            delay(300)
            contentVisible = true
        }
    }

    Scaffold { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = indicatorVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ServiceListContent(
                    formState = formState,
                    onBuildingSelected = viewModel::onBuildingChange,
                    onEdit = viewModel::onEditService,
                    onDelete = viewModel::onDeleteService,
                    navController = navController,
                    isLoading = uiState is UiState.Loading
                )
            }

            if (uiState is UiState.Error) {
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
        }
    }
}


@Composable
fun ServiceListContent (
    formState: ServiceListFormState,
    onEdit: (Service) -> Unit,
    onDelete: (Service) -> Unit,
    onBuildingSelected: (Building) -> Unit,
    navController: NavHostController,
    isLoading: Boolean = false
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                        enabled = formState.availableBuildings.isNotEmpty() && !isLoading
                    )
                }

                if (formState.availableServices.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No services available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(formState.availableServices) { service ->
                        ServiceCard(
                            service = service,
                            onEdit = onEdit,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { navController.navigate(Screens.AddService.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add service")
        }
    }
}