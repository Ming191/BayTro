package com.example.baytro.view.screens.building

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.BuildingCard
import com.example.baytro.view.components.BuildingListSkeleton
import com.example.baytro.view.components.CompactSearchBar
import com.example.baytro.view.components.EmptyBuildingState
import com.example.baytro.view.components.PaginationControls
import com.example.baytro.viewModel.BuildingListVM
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingListScreen(
    navController: NavHostController? = null,
    viewModel: BuildingListVM = koinViewModel()
) {
    val controller = navController ?: rememberNavController()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasLoadedOnce by viewModel.hasLoadedOnce.collectAsState()
    val buildings by viewModel.buildings.collectAsState()
    val scope = rememberCoroutineScope()

    // Start with loading visible, content hidden
    var showLoading by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(false) }
    var showEmptyState by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("BuildingListScreen", "Screen launched - Checking if need to load")
        if (!hasLoadedOnce) {
            Log.d("BuildingListScreen", "First time loading buildings")
            showLoading = true
            showContent = false
            showEmptyState = false
            viewModel.loadBuildings()
        } else {
            Log.d("BuildingListScreen", "Already loaded, showing existing content")
            showLoading = false
            if (buildings.isEmpty()) {
                showEmptyState = true
                showContent = false
            } else {
                showEmptyState = false
                showContent = true
            }
        }
    }

    LaunchedEffect(isLoading, hasLoadedOnce) {
        Log.d("BuildingListScreen", "State changed: isLoading=$isLoading, hasLoadedOnce=$hasLoadedOnce, buildings.size=${buildings.size}")

        if (!isLoading && hasLoadedOnce) {
            Log.d("BuildingListScreen", "Loading complete, transitioning to content")

            isRefreshing = false
            showLoading = false
            kotlinx.coroutines.delay(300)

            if (buildings.isEmpty()) {
                Log.d("BuildingListScreen", "No buildings, showing empty state")
                showEmptyState = true
                showContent = false
            } else {
                Log.d("BuildingListScreen", "Buildings found (${buildings.size}), showing content")
                showEmptyState = false
                showContent = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showLoading,
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Log.d("BuildingListScreen", "Rendering skeleton")
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BuildingListSkeleton(itemCount = 5)
                }
            }
        }

        //Empty state
        AnimatedVisibility(
            visible = showEmptyState,
            enter = fadeIn(animationSpec = tween(300))
        ) {
            Log.d("BuildingListScreen", "Rendering empty state")
            Box(modifier = Modifier.fillMaxSize()) {
                EmptyBuildingState(
                    onAddBuilding = { controller.navigate(Screens.BuildingAdd.route) }
                )

                FloatingActionButton(
                    onClick = { controller.navigate(Screens.BuildingAdd.route) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add building")
                }
            }
        }

        // Main content with buildings (wrapped in PullToRefresh)
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(300))
        ) {
            Log.d("BuildingListScreen", "Rendering content (buildings.size=${buildings.size})")
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        Log.d("BuildingListScreen", "Pull to refresh triggered")
                        isRefreshing = true
                        viewModel.refreshBuildings()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                BuildingListContent(
                    navController = controller,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildingListContent(
    navController: NavHostController,
    viewModel: BuildingListVM
) {
    val filteredBuildings by viewModel.filteredBuildings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val paginatedBuildings by viewModel.paginatedBuildings.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val hasNextPage by viewModel.hasNextPage.collectAsState()
    val hasPreviousPage by viewModel.hasPreviousPage.collectAsState()

    var showStatusMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CompactSearchBar(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholderText = "Search buildings...",
                trailingIcon = {
                    Box {
                        IconButton(
                            onClick = { showStatusMenu = true }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter by status",
                                tint = if (statusFilter != BuildingListVM.BuildingStatusFilter.ALL) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            BuildingListVM.BuildingStatusFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = filter.name.lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            color = if (filter == statusFilter) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setStatusFilter(filter)
                                        showStatusMenu = false
                                    },
                                    leadingIcon = if (filter == statusFilter) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!filteredBuildings.isEmpty()) {
                Log.d("BuildingListContent", "Rendering LazyColumn with ${paginatedBuildings.size} paginated items (page $currentPage of $totalPages)")

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        items = paginatedBuildings,
                        key = { _, item -> item.building.id }
                    ) { index, buildingWithStats ->
                        Log.d("BuildingListContent", "Item[$index] composing: ${buildingWithStats.building.name} (id: ${buildingWithStats.building.id})")

                        var visible by remember(buildingWithStats.building.id) {
                            Log.d("BuildingListContent", "Item[$index] remember created for ${buildingWithStats.building.name}")
                            mutableStateOf(true)
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
                            BuildingCard(
                                name = buildingWithStats.building.name,
                                address = buildingWithStats.building.address,
                                imageUrl = buildingWithStats.building.imageUrls.firstOrNull(),
                                roomStats = "${buildingWithStats.occupiedRooms}/${buildingWithStats.totalRooms}",
                                revenue = "$0",
                                onViewClick = {
                                    navController.navigate(Screens.RoomList.createRoute(buildingWithStats.building.id))
                                },
                                onEditClick = {
                                    navController.navigate(
                                        Screens.BuildingEdit.route.replace("{id}", buildingWithStats.building.id)
                                    )
                                }
                            )
                        }
                    }

                    // Pagination controls
                    if (totalPages > 1) {
                        item {
                            Log.d("BuildingListContent", "Rendering PaginationControls: page $currentPage of $totalPages")
                            PaginationControls(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                hasNextPage = hasNextPage,
                                hasPreviousPage = hasPreviousPage,
                                onNextPage = viewModel::nextPage,
                                onPreviousPage = viewModel::previousPage
                            )
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { navController.navigate(Screens.BuildingAdd.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add building")
        }
    }
}