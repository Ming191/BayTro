package com.example.baytro.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.baytro.data.Building
import com.example.baytro.navigation.Screens
import com.example.baytro.viewModel.BuildingListVM
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingListScreen(
    navController: NavHostController? = null,
    onViewBuilding: () -> Unit = {},
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // chừa chỗ cho FAB
                ) {
                    items(buildings) { b ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val firstImage = b.imageUrls.firstOrNull()
                                AsyncImage(
                                    model = firstImage ?: "https://via.placeholder.com/800x400?text=Building",
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                )
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
                                        OutlinedButton(onClick = onViewBuilding) {
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
