package com.example.baytro.view.screens.service

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.baytro.view.components.ServiceListSkeleton
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

    var showLoading by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(false) }

    Log.d("ServiceListScreen", "Recomposing - uiState: ${uiState::class.simpleName}, " +
            "showLoading: $showLoading, showContent: $showContent, " +
            "servicesCount: ${formState.availableServices.size}, " +
            "selectedBuilding: ${formState.selectedBuilding?.name}")

    // Only show skeleton on initial load, not when changing buildings
    LaunchedEffect(uiState) {
        Log.d("ServiceListScreen", "LaunchedEffect triggered - uiState changed to: ${uiState::class.simpleName}")

        when (uiState) {
            is UiState.Loading -> {
                Log.d("ServiceListScreen", "State: Loading - showContent=$showContent, showLoading=$showLoading")
                // Only show skeleton if we haven't loaded yet (initial load)
                if (!showContent && !showLoading) {
                    Log.d("ServiceListScreen", "First time loading - showing skeleton")
                    showLoading = true
                } else {
                    Log.d("ServiceListScreen", "Already loaded before - skipping skeleton (content visible)")
                }
            }
            is UiState.Success -> {
                Log.d("ServiceListScreen", "State: Success - services count: ${(uiState as UiState.Success).data.size}")
                showLoading = false
                delay(300)
                showContent = true
                Log.d("ServiceListScreen", "Content visible after delay")
            }
            is UiState.Error -> {
                Log.e("ServiceListScreen", "State: Error - ${(uiState as UiState.Error).message}")
                showLoading = false
                delay(300)
                showContent = true
            }
            is UiState.Idle -> {
                Log.d("ServiceListScreen", "State: Idle - keeping current state")
            }
            is UiState.Waiting -> {
                Log.d("ServiceListScreen", "State: Waiting - keeping current state")
            }
        }
    }

    Scaffold { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Only show skeleton for initial load
            AnimatedVisibility(
                visible = showLoading && !showContent,
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Log.d("ServiceListScreen", "Rendering skeleton loading")
                Surface(modifier = Modifier.fillMaxSize()) {
                    ServiceListSkeleton(itemCount = 5)
                }
            }

            // Show content after initial load (remains visible when changing buildings)
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(300))
            ) {
                Log.d("ServiceListScreen", "Rendering content with ${formState.availableServices.size} services")
                ServiceListContent(
                    formState = formState,
                    onBuildingSelected = { building ->
                        Log.d("ServiceListScreen", "Building selected: ${building.name} (id: ${building.id})")
                        viewModel.onBuildingChange(building)
                    },
                    onEdit = { service ->
                        Log.d("ServiceListScreen", "Edit service: ${service.name}")
                        viewModel.onEditService(service)
                    },
                    onDelete = { service ->
                        Log.d("ServiceListScreen", "Delete service: ${service.name}")
                        viewModel.onDeleteService(service)
                    },
                    navController = navController,
                    isLoading = uiState is UiState.Loading
                )
            }

            if (uiState is UiState.Error) {
                val message = (uiState as UiState.Error).message
                Log.e("ServiceListScreen", "Showing error dialog: $message")
                AlertDialog(
                    onDismissRequest = {
                        Log.d("ServiceListScreen", "Error dialog dismissed")
                        viewModel.clearError()
                    },
                    title = { Text("Notice") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = {
                            Log.d("ServiceListScreen", "Error dialog confirmed")
                            viewModel.clearError()
                        }) {
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
    Log.d("ServiceListContent", "Rendering - servicesCount: ${formState.availableServices.size}, " +
            "selectedBuilding: ${formState.selectedBuilding?.name}, isLoading: $isLoading")

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
                        onOptionSelected = { building ->
                            Log.d("ServiceListContent", "Dropdown selection: ${building.name}")
                            onBuildingSelected(building)
                        },
                        optionToString = { it.name },
                        enabled = formState.availableBuildings.isNotEmpty() && !isLoading
                    )
                }

                if (formState.availableServices.isEmpty() && !isLoading) {
                    Log.d("ServiceListContent", "Showing empty state")
                    item {
                        var emptyStateVisible by remember { mutableStateOf(false) }

                        LaunchedEffect(formState.selectedBuilding?.id) {
                            Log.d("ServiceListContent", "Empty state animation - building changed to: ${formState.selectedBuilding?.name}")
                            emptyStateVisible = false
                            delay(100)
                            emptyStateVisible = true
                            Log.d("ServiceListContent", "Empty state now visible")
                        }

                        AnimatedVisibility(
                            visible = emptyStateVisible,
                            enter = fadeIn(animationSpec = tween(400)),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
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
                    }
                } else {
                    Log.d("ServiceListContent", "Rendering ${formState.availableServices.size} service cards")
                    itemsIndexed(
                        items = formState.availableServices,
                        key = { _, service -> "${formState.selectedBuilding?.id}-${service.name}-${service.price}" }
                    ) { index, service ->
                        var visible by remember { mutableStateOf(false) }

                        LaunchedEffect(service, formState.selectedBuilding?.id) {
                            Log.d("ServiceListContent", "Service card [$index] animation started - ${service.name} for building ${formState.selectedBuilding?.name}")
                            visible = false
                            // Staggered delay based on index
                            val delayTime = index * 50L
                            Log.d("ServiceListContent", "Service card [$index] waiting ${delayTime}ms before appearing")
                            delay(delayTime)
                            visible = true
                            Log.d("ServiceListContent", "Service card [$index] now visible - ${service.name}")
                        }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialAlpha = 0f
                            ) + slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialOffsetY = { it / 3 }
                            )
                        ) {
                            ServiceCard(
                                service = service,
                                onEdit = {
                                    Log.d("ServiceListContent", "Edit clicked for service: ${service.name}")
                                    onEdit(it)
                                },
                                onDelete = {
                                    Log.d("ServiceListContent", "Delete clicked for service: ${service.name}")
                                    onDelete(it)
                                }
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = {
                Log.d("ServiceListContent", "FAB clicked - navigating to AddService")
                navController.navigate(Screens.AddService.route)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add service")
        }
    }
}