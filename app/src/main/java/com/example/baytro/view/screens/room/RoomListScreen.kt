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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.baytro.R
import com.example.baytro.data.Building
import com.example.baytro.data.Floor
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.ButtonComponent
import com.example.baytro.view.components.CardComponent
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.viewModel.Room.RoomListVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ViewBuildingTabRow(
    tabItemList: List<Pair<String, ImageVector>>,
    floors : List<Floor>,
    navController : NavHostController,
    building : Building?
) {
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
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabItemList.forEachIndexed { index, tabItem ->
                Tab(
                    text = { Text(tabItem.first) },
                    icon = { Icon(tabItem.second, contentDescription = tabItem.first) },
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) { index ->
            when (index) {
                0 -> ViewRoomList(floors, navController,building?.name)
                1 -> ViewBuildingDetails(navController, building)
            }
        }
    }
}

@Composable
fun ViewRoomList(
    floors : List<Floor>,
    navController : NavController,
    buildingName: String?
) {
    var expandedFloorNumber by remember { mutableIntStateOf(-1) }
    Log.d("RoomList", "BuildingNameInRoomList: $buildingName")
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
                            floor.rooms.forEach { room ->
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
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        //add room
        FloatingActionButton(
            onClick = {navController.navigate(Screens.AddRoom.createRoute(buildingName))},
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
    building: Building?
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
        CardComponent(
            infoMap = mapOf(
                "Num.Rooms" to "12",
                "Num.Tenants" to "12",
                "Num.Floors" to building?.floor.toString(),
                "Address" to building?.address.toString(),
                "Billing date" to building?.billingDate.toString(),
                "Payment start" to building?.paymentStart.toString(),
                "Payment due" to building?.paymentDue.toString()
            )
        )
        DividerWithSubhead(subhead = "Building photo")
        Image(
            painter = painterResource(id = R.drawable.building_img),
            contentDescription = "image description",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(380.dp)
                .height(188.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.Top,
        ) {
            ButtonComponent(text = "Edit", onButtonClick = {})
            Spacer(Modifier.width(8.dp))
            ButtonComponent (text = "Delete", onButtonClick = {})
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
    LaunchedEffect(Unit) {
        viewModel.fetchBuilding()
        viewModel.fetchRooms()
    }
    ViewBuildingTabRow(
        tabItemList = listOf(
            "Room list" to Icons.Outlined.List,
            "Details" to Icons.Outlined.Info
        ),
        floors = floors,
        navController = navController,
        building = building
    )
}

@Preview
@Composable
fun Preview() {
    //ViewRoomList()
}