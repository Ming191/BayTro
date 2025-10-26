package com.example.baytro.view.screens.room

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.baytro.R
import com.example.baytro.data.Building
import com.example.baytro.data.room.Floor
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.Status
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.Tabs
import com.example.baytro.viewModel.Room.RoomListVM
import org.koin.compose.viewmodel.koinViewModel

// Updated ViewBuildingTabRow: Removed Scaffold and innerPadding
@Composable
fun ViewBuildingTabRow(
    tabItemList: List<Pair<String, ImageVector>>,
    floors: List<Floor>,
    navController: NavHostController,
    building: Building?,
    onEditBuilding: (String) -> Unit,
    onDeleteBuilding: (String) -> Unit,
    buildingTenants: List<String>,
    isLoadingRooms: Boolean = false,
    isRefreshing: Boolean = false,
    roomTenants: Map<String, List<String>> = emptyMap(),
    currentTabIndex: Int,
    onRefresh: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0) { tabItemList.size }

    LaunchedEffect(selectedTabIndex) {
        pagerState.animateScrollToPage(selectedTabIndex)
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    LaunchedEffect(currentTabIndex) {
        if (currentTabIndex != selectedTabIndex) {
            selectedTabIndex = currentTabIndex
        }
    }


    Column(Modifier.fillMaxSize()) {
        Tabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { index -> selectedTabIndex = index },
            tabData = tabItemList
        )
        HorizontalPager(
            state = pagerState,
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) { index ->
            when (index) {
                0 -> ViewRoomList(
                    floors = floors,
                    navController = navController,
                    buildingId = building?.id,
                    isLoading = isLoadingRooms,
                    isRefreshing = isRefreshing,
                    roomTenants = roomTenants,
                    onRefresh = onRefresh
                )
                1 -> ViewBuildingDetails(
                    navController = navController,
                    building = building,
                    floors = floors,
                    onEdit = { building?.id?.let(onEditBuilding) },
                    onDelete = { building?.id?.let(onDeleteBuilding) },
                    buildingTenants = buildingTenants
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewRoomList(
    floors: List<Floor>,
    navController: NavController,
    buildingId: String?,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    roomTenants: Map<String, List<String>> = emptyMap(),
    onRefresh: () -> Unit = {}
) {
    var expandedFloorNumber by remember { mutableIntStateOf(-1) }
    val pullToRefreshState = rememberPullToRefreshState()

    val totalRooms = floors.sumOf { floor ->
        floor.rooms.count { room -> room.status != Status.ARCHIVED}
    }
    val occupiedRooms = floors.sumOf { floor ->
        floor.rooms.count { room -> room.status != Status.ARCHIVED && roomTenants[room.id]?.isNotEmpty() == true }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && !isRefreshing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Loading rooms...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please wait",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (floors.isEmpty() || totalRooms == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MeetingRoom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No rooms yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Start by creating your first room\nfor this building",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { navController.navigate(Screens.AddRoom.createRoute(buildingId)) },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                    Text("Add First Room")
                }
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Summary card
                    item {
                        SummaryCard(
                            totalFloors = floors.size,
                            totalRooms = totalRooms,
                            occupiedRooms = occupiedRooms
                        )
                    }
                    items(floors) { floor ->
                        FloorCard(
                            floor = floor,
                            isExpanded = expandedFloorNumber == floor.number,
                            onExpandToggle = {
                                expandedFloorNumber = if (expandedFloorNumber == floor.number) -1 else floor.number
                            },
                            onRoomClick = { roomId ->
                                navController.navigate(Screens.RoomDetails.createRoute(roomId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    totalFloors: Int,
    totalRooms: Int,
    occupiedRooms: Int
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                icon = Icons.Filled.Layers,
                value = totalFloors.toString(),
                label = "Floors"
            )
            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            SummaryItem(
                icon = Icons.Filled.MeetingRoom,
                value = totalRooms.toString(),
                label = "Total Rooms"
            )
            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            SummaryItem(
                icon = Icons.Filled.Person,
                value = occupiedRooms.toString(),
                label = "Occupied"
            )
        }
    }
}

@Composable
fun SummaryItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FloorCard(
    floor: Floor,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onRoomClick: (String) -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrow rotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Floor header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Floor ${floor.number}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${floor.rooms.size} rooms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .padding(4.dp)
                            .rotate(rotationAngle)
                    )
                }
            }

            // Expandable room list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    floor.rooms.sortedBy { it.roomNumber }.forEach { room ->
                        RoomListItem(
                            room = room,
                            onClick = { onRoomClick(room.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomListItem(
    room: Room,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.MeetingRoom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = "Room ${room.roomNumber}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ViewBuildingDetails(
    navController: NavHostController,
    building: Building?,
    floors: List<Floor>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    buildingTenants: List<String>
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val totalRooms = floors.sumOf { it.rooms.size }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Building Photos Section
        item {
            BuildingPhotosSection(building = building)
        }

        // Quick Stats Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Filled.MeetingRoom,
                    value = totalRooms.toString(),
                    label = "Rooms",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.Person,
                    value = buildingTenants.size.toString(),
                    label = "Tenants",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.Layers,
                    value = building?.floor?.toString() ?: "0",
                    label = "Floors",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Building Information Section
        item {
            Text(
                text = "Building Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoRow(
                        icon = Icons.Filled.LocationOn,
                        label = "Address",
                        value = building?.address ?: "Not specified"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    InfoRow(
                        icon = Icons.Filled.DateRange,
                        label = "Billing Date",
                        value = building?.billingDate?.toString() ?: "Not set"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    InfoRow(
                        icon = Icons.Filled.CalendarToday,
                        label = "Payment Start",
                        value = building?.paymentStart?.toString() ?: "Not set"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    InfoRow(
                        icon = Icons.Filled.Event,
                        label = "Payment Due",
                        value = building?.paymentDue?.toString() ?: "Not set"
                    )
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Building")
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Building?") },
            text = {
                Text("This action cannot be undone. All rooms and associated data will be permanently deleted.")
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BuildingPhotosSection(building: Building?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (building?.imageUrls?.isNotEmpty() == true) {
            val photoPagerState = rememberPagerState(initialPage = 0) { building.imageUrls.size }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                HorizontalPager(
                    state = photoPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(building.imageUrls[page])
                            .crossfade(400)
                            .build(),
                        contentDescription = "Building image ${page + 1}",
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        },
                        error = {
                            Image(
                                painter = painterResource(id = R.drawable.building_img),
                                contentDescription = "Fallback image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )

                // Page indicators
                if (building.imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(building.imageUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(
                                        if (photoPagerState.currentPage == index) 24.dp else 8.dp,
                                        8.dp
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (photoPagerState.currentPage == index)
                                            Color.White
                                        else
                                            Color.White.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "No photos available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RoomListScreen(
    navController: NavHostController,
    viewModel: RoomListVM = koinViewModel(),
) {
    val floors by viewModel.floors.collectAsState()
    val building by viewModel.building.collectAsState()
    val buildingTenants by viewModel.buildingTenants.collectAsState()
    val isLoadingRooms by viewModel.isLoadingRooms.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val roomTenants by viewModel.roomTenants.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Listen for results from Add/Edit/Delete Room operations
    val currentBackStackEntry = navController.currentBackStackEntry
    val roomModified = currentBackStackEntry?.savedStateHandle?.get<Boolean>("room_modified")

    LaunchedEffect(roomModified) {
        if (roomModified == true) {
            viewModel.refresh()
            currentBackStackEntry.savedStateHandle.remove<Boolean>("room_modified")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchBuilding()
        viewModel.fetchRooms()
        viewModel.fetchBuildingTenants()
    }

    val totalRooms = remember(floors) { floors.sumOf { it.rooms.size } }

    val showFab = remember(selectedTab, isLoadingRooms, floors, totalRooms) {
        selectedTab == 0 && !isLoadingRooms && (floors.isNotEmpty() && totalRooms > 0)
    }

    Scaffold(
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screens.AddRoom.createRoute(building?.id)) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Room"
                    )
                }
            }
        }
    ) {
        ViewBuildingTabRow(
            tabItemList = listOf(
                "Room list" to Icons.AutoMirrored.Outlined.List,
                "Building details" to Icons.Outlined.Info
            ),
            floors = floors,
            navController = navController,
            building = building,
            onEditBuilding = { id -> navController.navigate(Screens.BuildingEdit.createRoute(id)) },
            onDeleteBuilding = { id -> viewModel.deleteBuilding(id) },
            buildingTenants = buildingTenants,
            isLoadingRooms = isLoadingRooms,
            isRefreshing = isRefreshing,
            roomTenants = roomTenants,
            currentTabIndex = selectedTab,
            onRefresh = { viewModel.refresh() }
        )

    }
}