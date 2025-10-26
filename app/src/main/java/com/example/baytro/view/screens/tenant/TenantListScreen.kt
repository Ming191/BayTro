package com.example.baytro.view.screens.tenant

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.CompactSearchBar
import com.example.baytro.view.components.TenantListSkeleton
import com.example.baytro.viewModel.tenant.TenantListVM
import com.example.baytro.viewModel.tenant.TenantDisplay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TenantListScreen(
    onViewTenantInfoClick: (String) -> Unit,
    viewModel: TenantListVM = koinViewModel()
) {
    val filteredTenantList by viewModel.filteredTenantList.collectAsState()
    val tenantList by viewModel.tenantList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTenant()
        Log.d("TenantListScreen", "fetch tenant list")
    }
    LaunchedEffect(tenantList) {
        if (tenantList.isNotEmpty()) {
            viewModel.getTenantRoomAndBuilding()
            Log.d("TenantListScreen", "fetch tenant room and building done")
        }
    }

    Log.d("TenantListScreen", "Filtered tenant list size: ${filteredTenantList.size}")

    // Show skeleton while tenantList is empty (loading). Replace with viewModel.isLoading if you add that flag.
    val isLoading = tenantList.isEmpty()

    if (isLoading) {
        TenantListSkeleton(itemCount = 6)
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            item {
                CompactSearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    value = searchQuery,
                    onValueChange = { viewModel.searchingQuery(it) },
                    placeholderText = "Search tenants..."
                )
            }
            items(filteredTenantList) { display: TenantDisplay ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                Log.d("TenantListScreen", "Tenant clicked: ${display.tenant.fullName}")
                                onViewTenantInfoClick(display.tenant.id)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AsyncImage(
                            model = display.tenant.profileImgUrl,
                            contentDescription = display.tenant.fullName,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = display.tenant.fullName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Column {
                                Text(
                                    text = display.tenant.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${display.room.roomNumber} - ${display.building.name}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Tenant Info",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TenantListScreenPreview() {
    //TenantListScreen(navController = NavHostController(LocalContext.current))
}