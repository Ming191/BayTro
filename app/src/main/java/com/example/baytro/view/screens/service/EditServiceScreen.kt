package com.example.baytro.view.screens.service

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.service.Metric
import com.example.baytro.data.service.Service
import com.example.baytro.view.components.CompactSearchBar
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.service.EditServiceFormState
import com.example.baytro.viewModel.service.EditServiceVM
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditServiceScreen(
    navController: NavHostController,
    viewModel: EditServiceVM = koinViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    EditServiceContent(
        uiState = uiState,
        formState = formState,
        onNameChange = { viewModel.onNameChange(it) },
        onPriceChange = { viewModel.onPriceChange(it) },
        onUnitSelected = { viewModel.onUnitChange(it) },
        onToggleRoom = { viewModel.onToggleRoom(it) },
        onToggleSelectAll = { viewModel.onToggleSelectAll() },
        onSearchTextChange = { viewModel.onSearchTextChange(it) },
        onConfirm = { viewModel.onConfirm() },
        isLoading = uiState is UiState.Loading
    )

    when (uiState) {
        is UiState.Success -> {
            Toast.makeText(LocalContext.current, "Updated service", Toast.LENGTH_SHORT).show()
            // Set flag to trigger refresh in ServiceListScreen
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("service_modified", true)
            navController.popBackStack()
            viewModel.clearError()
        }
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
        else -> {}
    }
}

@Composable
fun EditServiceContent(
    uiState: UiState<Service>,
    formState: EditServiceFormState,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUnitSelected: (Metric) -> Unit,
    onToggleRoom: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onSearchTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false
) {
    var nameFieldVisible by remember { mutableStateOf(false) }
    var priceFieldVisible by remember { mutableStateOf(false) }
    var metricsFieldVisible by remember { mutableStateOf(false) }
    var roomsSectionVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        nameFieldVisible = true
        priceFieldVisible = true
        metricsFieldVisible = true
        roomsSectionVisible = true
    }

    Scaffold(
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(
                        bottom = paddingValues.calculateBottomPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = nameFieldVisible,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = onNameChange,
                        label = { Text("Service name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !formState.isDefault,
                    )
                }

                AnimatedVisibility(
                    visible = priceFieldVisible,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    OutlinedTextField(
                        value = formState.price,
                        onValueChange = onPriceChange,
                        label = { Text("Unit price") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }

                AnimatedVisibility(
                    visible = metricsFieldVisible,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    DropdownSelectField(
                        label = "Metrics",
                        options = Metric.entries.toList(),
                        selectedOption = formState.metrics,
                        onOptionSelected = onUnitSelected,
                        optionToString = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !formState.isDefault
                    )
                }

                AnimatedVisibility(
                    visible = roomsSectionVisible,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    if (formState.availableRooms.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var dividerVisible by remember { mutableStateOf(false) }
                            var searchBarVisible by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                dividerVisible = true
                                searchBarVisible = true
                            }

                            AnimatedVisibility(
                                visible = dividerVisible,
                                enter = fadeIn(animationSpec = tween(300))
                            ) {
                                HorizontalDivider(
                                    thickness = 2.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = searchBarVisible,
                                enter = fadeIn(animationSpec = tween(300))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CompactSearchBar(
                                        value = formState.searchText,
                                        onValueChange = onSearchTextChange,
                                        placeholderText = "Search rooms...",
                                        modifier = Modifier.weight(1f),
                                        enabled = !isLoading
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = formState.selectedRooms.size == formState.availableRooms.size && formState.availableRooms.isNotEmpty(),
                                            onCheckedChange = { onToggleSelectAll() },
                                            enabled = !isLoading
                                        )
                                        Text("Select all")
                                    }
                                }
                            }

                            val filteredRooms = formState.availableRooms.filter { room ->
                                room.roomNumber.contains(formState.searchText, ignoreCase = true)
                            }
                            var currentBuildingId by remember { mutableStateOf(formState.selectedBuilding?.id) }
                            var roomsVisible by remember { mutableStateOf(false) }

                            LaunchedEffect(formState.selectedBuilding?.id) {
                                if (currentBuildingId != formState.selectedBuilding?.id && currentBuildingId != null) {
                                    roomsVisible = false
                                }
                                currentBuildingId = formState.selectedBuilding?.id
                                if (filteredRooms.isNotEmpty()) {
                                    roomsVisible = true
                                }
                            }

                            AnimatedVisibility(
                                visible = roomsVisible && filteredRooms.isNotEmpty(),
                                enter = fadeIn(animationSpec = tween(400)),
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(
                                        items = filteredRooms,
                                        key = { _, room -> "${formState.selectedBuilding?.id}-${room.id}" }
                                    ) { index, room ->
                                        var visible by remember { mutableStateOf(false) }

                                        LaunchedEffect(room.id, formState.selectedBuilding?.id) {
                                            visible = false
                                            visible = true
                                        }

                                        AnimatedVisibility(
                                            visible = visible,
                                            enter = fadeIn(animationSpec = tween(250))
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                onClick = { onToggleRoom(room.id) },
                                                enabled = !isLoading
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                                ) {
                                                    Text(
                                                        text = room.roomNumber,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Checkbox(
                                                        checked = formState.selectedRooms.contains(room.id),
                                                        onCheckedChange = { onToggleRoom(room.id) },
                                                        enabled = !isLoading
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (formState.selectedBuilding != null) {
                        var emptyMessageVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            emptyMessageVisible = true
                        }

                        AnimatedVisibility(
                            visible = emptyMessageVisible,
                            enter = fadeIn(animationSpec = tween(300))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                HorizontalDivider(
                                    thickness = 2.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Text(
                                    text = "Service will be added to building service list.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SubmitButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 16.dp),
                    isLoading = uiState is UiState.Loading,
                    onClick = { onConfirm() }
                )
            }
        }
    )
}