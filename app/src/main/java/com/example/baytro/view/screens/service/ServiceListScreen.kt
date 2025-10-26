package com.example.baytro.view.screens.service

import android.annotation.SuppressLint
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ServiceListScreen(
    navController: NavHostController,
    viewModel: ServiceListVM = koinViewModel()
) {
    val uiState by viewModel.serviceListUiState.collectAsState()
    val formState by viewModel.serviceListFormState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val showSkeleton = uiState is UiState.Loading && formState.availableServices.isEmpty()
    val showContent = !showSkeleton

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("ServiceListScreen", "ON_RESUME - refreshing buildings")
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold (
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Log.d("ServiceListContent", "FAB clicked - navigating to AddService")
                    navController.navigate(Screens.AddService.createRoute("", "",false))
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add service")
            }
        }
    ) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = showSkeleton,
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Log.d("ServiceListScreen", "Rendering skeleton loading")
                Surface(modifier = Modifier.fillMaxSize()) {
                    ServiceListSkeleton(itemCount = 5)
                }
            }

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 100))
            ) {
                Log.d("ServiceListScreen", "Rendering content")
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
                Log.e("ServiceListScreen", "Showing error dialog: $message")
                AlertDialog(
                    onDismissRequest = viewModel::clearError,
                    title = { Text("Notice") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = viewModel::clearError) {
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
                        key = { _, service -> service.id }
                    ) { index, service ->
                        var visible by remember { mutableStateOf(false) }

                        LaunchedEffect(service, formState.selectedBuilding?.id) {
                            Log.d("ServiceListContent", "Service card [$index] animation started - ${service.name} for building ${formState.selectedBuilding?.name}")
                            visible = false
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
                                    navController.navigate(Screens.EditService.createRoute(formState.selectedBuilding?.id.toString(), service.id))
                                },
                                onDelete = {
                                    Log.d(
                                        "ServiceListContent",
                                        "Delete clicked unable for service: ${service.name}"
                                    )
                                    onDelete(it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}