package com.example.baytro.view.screens.building

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.navigation.Screens
import com.example.baytro.utils.Utils
import com.example.baytro.view.components.BuildingCard
import com.example.baytro.view.components.BuildingListSkeleton
import com.example.baytro.view.components.CompactSearchBar
import com.example.baytro.view.components.EmptyBuildingState
import com.example.baytro.viewModel.BuildingListVM
import com.example.baytro.viewModel.BuildingStatusFilter
import org.koin.compose.viewmodel.koinViewModel

private enum class LoadingState {
    LOADING, CONTENT, EMPTY
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BuildingListScreen(
    navController: NavHostController,
    viewModel: BuildingListVM = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let { errorMessage ->
                snackbarHostState.showSnackbar(message = errorMessage)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CompactSearchBar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholderText = "Search buildings...",
                    trailingIcon = {
                        FilterMenu(
                            selectedFilter = uiState.statusFilter,
                            onFilterSelected = { viewModel.setStatusFilter(it) },
                            hasActiveFilter = uiState.statusFilter != BuildingStatusFilter.ALL
                        )
                    }
                )

                AnimatedVisibility(
                    visible = uiState.statusFilter != BuildingStatusFilter.ALL,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Filter: ${uiState.statusFilter.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Crossfade(
                    targetState = when {
                        uiState.isLoading -> LoadingState.LOADING
                        uiState.buildings.isNotEmpty() -> LoadingState.CONTENT
                        else -> LoadingState.EMPTY
                    },
                    label = "buildingListCrossfade",
                    animationSpec = tween(durationMillis = 300)
                ) { state ->
                    when (state) {
                        LoadingState.LOADING -> {
                            Box(modifier = Modifier.padding(16.dp)) {
                                BuildingListSkeleton()
                            }
                        }
                        LoadingState.CONTENT -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 100.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = uiState.buildings,
                                    key = { it.building.id }
                                ) { buildingWithStats ->
                                    BuildingCard(
                                        name = buildingWithStats.building.name,
                                        address = buildingWithStats.building.address,
                                        imageUrl = buildingWithStats.building.imageUrls.firstOrNull(),
                                        roomStats = "${buildingWithStats.occupiedRooms}/${buildingWithStats.totalRooms}",
                                        revenue = Utils.formatCurrency(buildingWithStats.revenue.toString()),
                                        onViewClick = {
                                            navController.navigate(
                                                Screens.RoomList.createRoute(buildingWithStats.building.id)
                                            )
                                        },
                                        onEditClick = {
                                            navController.navigate(
                                                Screens.BuildingEdit.createRoute(buildingWithStats.building.id)
                                            )
                                        },
                                        onDeleteClick = {
                                            viewModel.archiveBuilding(buildingWithStats.building.id)
                                        }
                                    )
                                }
                            }
                        }
                        LoadingState.EMPTY -> {
                            EmptyBuildingState(
                                onAddBuilding = { navController.navigate(Screens.BuildingAdd.route) }
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, end = 20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screens.BuildingAdd.route) },
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        "Add Building",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(snackbarHostState)
        }
    }
}

@Composable
private fun FilterMenu(
    selectedFilter: BuildingStatusFilter,
    onFilterSelected: (BuildingStatusFilter) -> Unit,
    hasActiveFilter: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (showMenu) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "filterRotation"
    )

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (hasActiveFilter)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = if (hasActiveFilter) 2.dp else 0.dp
        ) {
            BadgedBox(
                badge = {
                    if (hasActiveFilter) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter by status",
                        tint = if (hasActiveFilter)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(rotation)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = "Filter Buildings",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            BuildingStatusFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (filter == selectedFilter) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (filter == selectedFilter)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onFilterSelected(filter)
                        showMenu = false
                    },
                    leadingIcon = if (filter == selectedFilter) {
                        {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        { Spacer(modifier = Modifier.size(32.dp)) }
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}