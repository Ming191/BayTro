package com.example.baytro.view.screens.room

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.baytro.R
import com.example.baytro.data.Building
import com.example.baytro.data.room.Floor
import com.example.baytro.data.room.Room
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.ButtonComponent
import com.example.baytro.view.components.CardComponent
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.Tabs
import com.example.baytro.viewModel.Room.RoomListVM
import org.koin.compose.viewmodel.koinViewModel
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun ViewBuildingTabRow(
    tabItemList: List<Pair<String, ImageVector>>,
    floors : List<Floor>,
    navController : NavHostController,
    building : Building?,
    onEditBuilding: (String) -> Unit,
    onDeleteBuilding: (String) -> Unit,
    buildingTenants : List<String>,
    rooms : List<Room>
) {
    Log.d("BuildingTabRow", "BuildingName: ${building?.id}")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState (initialPage = 0){tabItemList.size}
    LaunchedEffect(selectedTabIndex) {
        pagerState.animateScrollToPage(selectedTabIndex)
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            selectedTabIndex = pagerState.currentPage
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
                0 -> ViewRoomList(floors, navController, building?.id)
                1 -> ViewBuildingDetails(
                    navController = navController,
                    building = building,
                    floors = floors,
                    onEdit = { building?.id?.let(onEditBuilding) },
                    onDelete = { building?.id?.let(onDeleteBuilding) },
                    buildingTenants = buildingTenants,
                    rooms = rooms
                )
            }
        }
    }
}

@Composable
fun ViewRoomList(
    floors : List<Floor>,
    navController : NavController,
    buildingId: String?
) {
    var expandedFloorNumber by remember { mutableIntStateOf(-1) }
    Log.d("RoomList", "BuildingIdInRoomList: $buildingId")
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxSize()
        ) {
            items(floors) { floor ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp) // spacing floor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedFloorNumber = if (expandedFloorNumber == floor.number) -1 else floor.number
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Floor ${floor.number}")
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = if (expandedFloorNumber == floor.number) "Collapse floor" else "Expand floor"
                        )
                    }
                    AnimatedVisibility(
                        visible = expandedFloorNumber == floor.number,
                        enter = expandVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            floor.rooms.sortedBy { it.roomNumber }.forEach { room ->
                                ListItem(
                                    headlineContent = { Text("Room ${room.roomNumber}") },
                                    leadingContent = {
                                        Icon(
                                            Icons.Outlined.Person,
                                            contentDescription = "Room ID"
                                        )
                                    },
                                    trailingContent = {
                                        Icon(
                                            Icons.Outlined.KeyboardArrowRight,
                                            contentDescription = "Room Info"
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screens.RoomDetails.createRoute(room.id ))
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        //add room
        FloatingActionButton(
            onClick = {navController.navigate(Screens.AddRoom.createRoute(buildingId))},
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add room")
        }
    }
}

@Composable
fun ViewBuildingDetails(
    navController : NavHostController,
    building: Building?,
    floors: List<Floor>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    buildingTenants: List<String>,
    rooms : List<Room>
) {
    Log.d("BuildingDetails", "BuildingName: ${building?.name}")
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    ) {
        DividerWithSubhead(subhead = "Information")
        val totalRooms = floors.sumOf { it.rooms.size }
        CardComponent(
            infoMap = mapOf(
                "Num.Rooms" to totalRooms.toString(),
                "Num.Tenants" to buildingTenants.size.toString(),
                "Num.Floors" to (building?.floor?.toString() ?: "-"),
                "Address" to (building?.address ?: "-"),
                "Billing date" to (building?.billingDate?.toString() ?: "-"),
                "Payment start" to (building?.paymentStart?.toString() ?: "-"),
                "Payment due" to (building?.paymentDue?.toString() ?: "-")
            )
        )
        DividerWithSubhead(subhead = "Building photo")
        val context = LocalContext.current
        if (building?.imageUrls?.isNotEmpty() == true) {
            val photoPagerState = rememberPagerState(initialPage = 0) { building.imageUrls.size }
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = photoPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) { page ->
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(building.imageUrls[page])
                            .crossfade(300)
                            .build(),
                        contentDescription = "Building image ${page + 1}",
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        },
                        error = {
                            Image(
                                painter = painterResource(id = R.drawable.building_img),
                                contentDescription = "fallback image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
                // Page indicator
                if (building.imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(building.imageUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (photoPagerState.currentPage == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        shape = androidx.compose.foundation.shape.CircleShape
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
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "No Image",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.Top,
        ) {
            ButtonComponent(text = "Edit", onButtonClick = { onEdit() })
            Spacer(Modifier.width(8.dp))
            var showDeleteConfirm by androidx.compose.runtime.mutableStateOf(false)
            if (showDeleteConfirm) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { androidx.compose.material3.Text("Delete building") },
                    text = { androidx.compose.material3.Text("Are you sure you want to delete this building? This action cannot be undone.") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            showDeleteConfirm = false
                            onDelete()
                            navController.popBackStack()
                        }) { androidx.compose.material3.Text("Delete") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) { androidx.compose.material3.Text("Cancel") }
                    }
                )
            }
            ButtonComponent (text = "Delete", onButtonClick = { showDeleteConfirm = true })
        }
    }
}

@Composable
fun RoomListScreen(
    navController: NavHostController,
    viewModel: RoomListVM = koinViewModel(),
) {
    val floors by viewModel.floors.collectAsState()
    val building by viewModel.building.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val buildingTenants by viewModel.buildingTenants.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchBuilding()
        viewModel.fetchRooms()
        viewModel.fetchBuildingTenants()
    }
    ViewBuildingTabRow(
        tabItemList = listOf(
            "Room list" to Icons.Outlined.List,
            "Building details" to Icons.Outlined.Info
        ),
        floors = floors,
        navController = navController,
        building = building,
        onEditBuilding = { id -> navController.navigate(Screens.BuildingEdit.createRoute(id)) },
        onDeleteBuilding = { id -> viewModel.deleteBuilding(id) },
        buildingTenants = buildingTenants,
        rooms = rooms
    )
}