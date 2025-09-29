package com.example.baytro.view.screens.room

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.ListItem
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.baytro.R
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.ButtonComponent
import com.example.baytro.view.components.DividerWithSubhead


data class Floor(val number: Int, val rooms: List<String>)
@Composable
fun TabRowComponent(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    Box(
        Modifier
            .width(412.dp)
            .height(48.dp)
            .background(Color.White)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            Modifier.background(Color.White)
        ) {
            LeadingIconTab(
                selected = selectedTabIndex == 0,
                onClick = {onTabSelected(0)},
                text = { Text(text = "Room list", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                icon = {
                    Icon(
                        Icons.Outlined.List,
                        contentDescription = "room list" // Add a valid content description
                    )
                }
            )
            LeadingIconTab(
                selected = selectedTabIndex == 1,
                onClick = {onTabSelected(1)},
                text = { Text(text = "Details", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                icon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "building details" // Add a valid content description
                    )
                }
            )
        }
    }
}

@Composable
fun ViewRoomList(floors : List<Floor>, navController : NavController) {
    var expandedFloorNumber by remember { mutableIntStateOf(-1) }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(floors) { floor ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedFloorNumber =
                                    if (expandedFloorNumber == floor.number) -1 else floor.number
                            }
                            .padding(end = 20.dp),
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
                                    headlineContent = { Text("Room $room") },
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
                                    modifier = Modifier.padding(horizontal = 16.dp) // Padding cho phòng
                                )
                            }
                        }
                    }
                }
            }
        }
        //add room
        FloatingActionButton(
            onClick = {navController.navigate(Screens.AddRoom.route)},
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add room")
        }
    }
}

@Composable
fun ViewBuildingDetails(navController : NavHostController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .width(380.dp)
            .wrapContentHeight()
            .padding(start = 8.dp)
    ) {
        DividerWithSubhead("Information")
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Num.Rooms:", modifier = Modifier.fillMaxWidth(1/3f))
                Spacer(modifier = Modifier.fillMaxWidth(5/19f))
                Text(text = "12")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Num.Tenants:", modifier = Modifier.fillMaxWidth(1/3f))
                Spacer(modifier = Modifier.fillMaxWidth(5/19f))
                Text(text = "12")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Num.Floors:", modifier = Modifier.fillMaxWidth(1/3f))
                Spacer(modifier = Modifier.fillMaxWidth(5/19f))
                Text(text = "12")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Address:", modifier = Modifier.fillMaxWidth(1/3f))
                Spacer(modifier = Modifier.fillMaxWidth(5/19f))
                Text(text = "12")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Billing date:", modifier = Modifier.fillMaxWidth(1/3f))
                Spacer(modifier = Modifier.fillMaxWidth(5/19f))
                Text(text = "12")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Rental fee:", modifier = Modifier.fillMaxWidth(1/3f))
                Spacer(modifier = Modifier.fillMaxWidth(5/19f))
                Text(text = "12")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Payment date:", modifier = Modifier.fillMaxWidth(1/3f))
                Spacer(modifier = Modifier.fillMaxWidth(5/19f))
                Text(text = "12")
            }
        }
        DividerWithSubhead("Building photo")
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
            ButtonComponent(text = "Edit", onButtonClick = { navController.navigate(Screens.EditRoom.route)})
            Spacer(Modifier.width(8.dp))
            ButtonComponent (text = "Delete", onButtonClick = {})
        }
    }
}

@Composable
fun RoomListScreen(
    navController: NavHostController
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val floors = remember {
        listOf(
            Floor(1, List(10) { "${101   + it}" }), // Tầng 1: 10 phòng
            Floor(2, List(8) { "${201 + it}" }),  // Tầng 2: 8 phòng
            Floor(3, List(12) { "${301 + it}" })  // Tầng 3: 12 phòng
        )
    }
    Column() {
        TabRowComponent(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it }
        )
        when (selectedTabIndex) {
            0 -> ViewRoomList(floors, navController)
            1 -> ViewBuildingDetails(navController)
        }
    }
}
@Preview
@Composable
fun Preview() {
    //ViewRoomList()
}