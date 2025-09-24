package com.example.baytro.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.baytro.navigation.Screens
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState

object Variables {
    val SchemesSurface: Color = Color(0xFFFFF8F8)
    val SchemesOutlineVariant: Color = Color(0xFFD4C2C7)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingListScreen(
    navController: NavHostController? = null,
    onViewBuilding: () -> Unit = {},
    onEditBuilding: () -> Unit = {},
    onAddBuilding: () -> Unit = {}
) {
    val controller = navController ?: rememberNavController()
    val buildings = remember { mutableStateListOf<Building>() }
    val savedStateHandle = controller.currentBackStackEntryAsState().value?.savedStateHandle
    LaunchedEffect(savedStateHandle?.get<Building>("new_building")) {
        savedStateHandle?.get<Building>("new_building")?.let { newItem ->
            buildings.add(0, newItem)
            savedStateHandle.remove<Building>("new_building")
        }
    }
    Box(
        modifier = Modifier
            .border(
                width = 8.dp,
                color = Variables.SchemesOutlineVariant,
                shape = RectangleShape
            )
            .padding(8.dp)
            .width(412.dp)
            .height(1055.dp)
            .background(color = Variables.SchemesSurface, shape = RoundedCornerShape(28.dp))
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxSize()
        ) {


            // Search bar
            SearchBar(
                query = "",
                onQueryChange = {},
                placeholder = { Text("Enter building name, address") },
                onSearch = {},
                active = false,
                onActiveChange = {},
                leadingIcon = { },
                trailingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {}

            Spacer(modifier = Modifier.height(8.dp))

            buildings.forEach { b ->
                Card(
                    modifier = Modifier
                        .width(380.dp)
                        .height(369.dp)
                        .padding(8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(text = b.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = b.address, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Rooms: 0/0")
                        Text(text = "Revenue: 0")

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(onClick = onViewBuilding) {
                                Text("View building")
                            }
                            IconButton(onClick = onEditBuilding) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                }
            }
        }

        // add building
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(onClick = {
                controller.navigate(Screens.BuildingAdd.route)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add building")
            }
        }
    }
}
