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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.baytro.navigation.Screens
import com.example.baytro.utils.Utils
import com.example.baytro.view.components.BuildingCard
import com.example.baytro.view.components.BuildingListSkeleton
import com.example.baytro.view.components.CompactSearchBar
import com.example.baytro.view.components.EmptyBuildingState
import com.example.baytro.viewModel.building.BuildingListVM
import com.example.baytro.viewModel.building.BuildingStatusFilter
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val loadingState by remember {
        derivedStateOf {
            when {
                uiState.isLoading -> LoadingState.LOADING
                uiState.buildings.isNotEmpty() -> LoadingState.CONTENT
                else -> LoadingState.EMPTY
            }
        }
    }

    val hasActiveFilter by remember {
        derivedStateOf { uiState.statusFilter != BuildingStatusFilter.ALL }
    }

    val filterDisplayText by remember {
        derivedStateOf {
            "Filter: ${uiState.statusFilter.name.lowercase().replaceFirstChar { it.uppercase() }}"
        }
    }

    val onAddBuildingClick = remember {
        { navController.navigate(Screens.BuildingAdd.route) }
    }

    val onSearchQueryChange = remember(viewModel) {
        { query: String -> viewModel.setSearchQuery(query) }
    }

    val onFilterSelected = remember(viewModel) {
        { filter: BuildingStatusFilter -> viewModel.setStatusFilter(filter) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let { errorMessage ->
                snackbarHostState.showSnackbar(message = errorMessage)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.successEvent.collect { event ->
            event.getContentIfNotHandled()?.let { successMessage ->
                snackbarHostState.showSnackbar(message = successMessage)
            }
        }
    }

    val isDeletingBuilding by viewModel.isDeletingBuilding.collectAsState()

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
                    onValueChange = onSearchQueryChange,
                    placeholderText = "Search buildings...",
                    trailingIcon = {
                        FilterMenu(
                            selectedFilter = uiState.statusFilter,
                            onFilterSelected = onFilterSelected,
                            hasActiveFilter = hasActiveFilter
                        )
                    }
                )

                AnimatedVisibility(
                    visible = hasActiveFilter,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .semantics {
                                contentDescription = "Active filter: ${uiState.statusFilter.name}"
                            },
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
                                contentDescription = "Filter icon",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = filterDisplayText,
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
                    targetState = loadingState,
                    label = "buildingListCrossfade",
                    animationSpec = tween(durationMillis = 300)
                ) { state ->
                    when (state) {
                        LoadingState.LOADING -> {
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .semantics { contentDescription = "Loading buildings" }
                            ) {
                                BuildingListSkeleton()
                            }
                        }
                        LoadingState.CONTENT -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .semantics { contentDescription = "Buildings list" },
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
                                    val buildingId = buildingWithStats.building.id
                                    val onViewClick = remember(buildingId) {
                                        { navController.navigate(Screens.RoomList.createRoute(buildingId)) }
                                    }
                                    val onEditClick = remember(buildingId) {
                                        { navController.navigate(Screens.BuildingEdit.createRoute(buildingId)) }
                                    }
                                    val onDeleteClick = remember(buildingId, viewModel) {
                                        { viewModel.archiveBuilding(buildingId) }
                                    }

                                    BuildingCard(
                                        name = buildingWithStats.building.name,
                                        address = buildingWithStats.building.address,
                                        imageUrl = buildingWithStats.building.imageUrls.firstOrNull(),
                                        roomStats = "${buildingWithStats.occupiedRooms}/${buildingWithStats.totalRooms}",
                                        revenue = Utils.formatCurrency(buildingWithStats.revenue.toString()),
                                        onViewClick = onViewClick,
                                        onEditClick = onEditClick,
                                        onDeleteClick = onDeleteClick
                                    )
                                }
                            }
                        }
                        LoadingState.EMPTY -> {
                            EmptyBuildingState(
                                onAddBuilding = onAddBuildingClick
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
            FloatingActionButton(
                onClick = onAddBuildingClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier.semantics { contentDescription = "Add new building" }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(snackbarHostState)
        }

        if (isDeletingBuilding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Deleting building...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
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

    val onIconClick = remember { { showMenu = true } }
    val onDismissRequest = remember { { showMenu = false } }

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (hasActiveFilter)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = if (hasActiveFilter) 2.dp else 0.dp,
            modifier = Modifier.semantics {
                contentDescription = if (hasActiveFilter) {
                    "Filter menu, active filter applied"
                } else {
                    "Filter menu"
                }
            }
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
                    onClick = onIconClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Open filter menu",
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
            onDismissRequest = onDismissRequest,
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
                val filterName = remember(filter) {
                    filter.name.lowercase().replaceFirstChar { it.uppercase() }
                }

                val onItemClick = remember(filter, onFilterSelected) {
                    {
                        onFilterSelected(filter)
                        showMenu = false
                    }
                }

                val isSelected = filter == selectedFilter

                DropdownMenuItem(
                    text = {
                        Text(
                            text = filterName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = onItemClick,
                    leadingIcon = if (isSelected) {
                        {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected filter",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        { Spacer(modifier = Modifier.size(32.dp)) }
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .semantics {
                            contentDescription = if (isSelected) {
                                "Filter by $filterName, currently selected"
                            } else {
                                "Filter by $filterName"
                            }
                        }
                )
            }
        }
    }
}