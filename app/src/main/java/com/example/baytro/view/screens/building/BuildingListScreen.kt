package com.example.baytro.view.screens.building
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.BuildingListSkeleton
import com.example.baytro.viewModel.BuildingListVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingListScreen(
    navController: NavHostController? = null,
    viewModel: BuildingListVM = koinViewModel()
) {
    val controller = navController ?: rememberNavController()
    val isLoading by viewModel.isLoading.collectAsState()
    // Load buildings when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadBuildings()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            val searchQuery by viewModel.searchQuery.collectAsState()
            val statusFilter by viewModel.statusFilter.collectAsState()
            var showStatusMenu by remember { mutableStateOf(false) }
            var isSearchFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isSearchFocused = focusState.isFocused
                        },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
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
                    },
                    placeholder = {
                        if (!isSearchFocused) {
                            Text("Search by name or address")
                        }
                    },
                    label = {
                        if (!isSearchFocused && searchQuery.isEmpty()) {
                            Text("Search by name or address")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val paginatedBuildings by viewModel.paginatedBuildings.collectAsState()
            val currentPage by viewModel.currentPage.collectAsState()
            val totalPages by viewModel.totalPages.collectAsState()
            val hasNextPage by viewModel.hasNextPage.collectAsState()
            val hasPreviousPage by viewModel.hasPreviousPage.collectAsState()
            val filteredBuildings by viewModel.filteredBuildings.collectAsState()
            val hasLoadedOnce by viewModel.hasLoadedOnce.collectAsState()

            when {
                isLoading -> {
                    BuildingListSkeleton(itemCount = 5)
                }
                filteredBuildings.isEmpty() && hasLoadedOnce -> {
                    // Empty state - only show after first load is complete
                    EmptyBuildingState(onAddBuilding = { controller.navigate(Screens.BuildingAdd.route) })
                }
                filteredBuildings.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(paginatedBuildings) { b ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val firstImage = b.imageUrls.firstOrNull()
                                    val context = LocalContext.current
                                    val configuration = LocalConfiguration.current
                                    val density = LocalDensity.current
                                    val widthPx =
                                        with(density) { configuration.screenWidthDp.dp.toPx() }.toInt()
                                    val heightPx = with(density) { 160.dp.toPx() }.toInt()

                                    if (firstImage != null) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(firstImage)
                                                .size(widthPx, heightPx)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .networkCachePolicy(CachePolicy.ENABLED)
                                                .crossfade(300) // Faster crossfade
                                                .allowHardware(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            loading = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(160.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(32.dp),
                                                        strokeWidth = 3.dp
                                                    )
                                                }
                                            },
                                            error = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(160.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            Icons.Default.BrokenImage,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(32.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            "Image unavailable",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                        )
                                    } else {
                                        // Better placeholder for no image
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Image,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "No Image",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = b.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = b.address,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Rooms: 0/0")
                                        Text(text = "Revenue: 0")
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedButton(onClick = {
                                                navController?.navigate(Screens.RoomList.createRoute(b.id))
                                            }) {
                                                Text("View building")
                                            }
                                            IconButton(onClick = {
                                                navController?.navigate(
                                                    Screens.BuildingEdit.route.replace("{id}", b.id)
                                                )
                                            }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Pagination controls - show when reaching end of current page
                        if (totalPages > 1) {
                            item {
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

        }

        // add building
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

@Composable
fun EmptyBuildingState(onAddBuilding: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "No Buildings Yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Start by adding your first building to manage your properties",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddBuilding,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Building")
            }
        }
    }
}

@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    hasNextPage: Boolean,
    hasPreviousPage: Boolean,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            IconButton(
                onClick = onPreviousPage,
                enabled = hasPreviousPage
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous page",
                    tint = if (hasPreviousPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Page info
            Text(
                text = "Page ${currentPage + 1} of $totalPages",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Next button
            IconButton(
                onClick = onNextPage,
                enabled = hasNextPage
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next page",
                    tint = if (hasNextPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}