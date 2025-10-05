package com.example.baytro.view.screens.building

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.baytro.navigation.Screens
import com.example.baytro.viewModel.BuildingListVM
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingListScreen(
    navController: NavHostController? = null,
    onViewBuilding: (String) -> Unit = {},
    onEditBuilding: () -> Unit = {},
    onAddBuilding: () -> Unit = {},
    viewModel: BuildingListVM = koinViewModel()
) {
    val controller = navController ?: rememberNavController()
    val buildings by viewModel.buildings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load buildings when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadBuildings()
    }
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

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
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
                            OutlinedButton(onClick = {
                                navController?.navigate(Screens.RoomList.createRoute(b.id))}) {
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
    }

    // add building
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(onClick = {
            controller.navigate(Screens.BuildingAdd.route)
        }) {
            Icon(Icons.Default.Add, contentDescription = "Add building")
        }
    }
}
