package com.example.baytro.view.screens.contract

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.baytro.data.Building
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PaginationControls
import com.example.baytro.view.components.Tabs
import com.example.baytro.viewModel.contract.ContractListVM
import com.example.baytro.viewModel.contract.ContractTab
import com.example.baytro.viewModel.contract.ContractWithRoom
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val tabData: List<Pair<String, ImageVector>> = listOf(
    "Active" to Icons.Filled.HomeWork,
    "Pending" to Icons.Filled.Schedule,
    "Ended" to Icons.Filled.CheckCircle
)

@Composable
fun ContractListScreen(
    viewModel: ContractListVM = koinViewModel(),
    onContractClick: (String) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val paginatedContracts by viewModel.paginatedContracts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val ownedBuildings by viewModel.ownedBuildings.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val hasNextPage by viewModel.hasNextPage.collectAsState()
    val hasPreviousPage by viewModel.hasPreviousPage.collectAsState()
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal, pageCount = { tabData.size })
    val scope = rememberCoroutineScope()

    ContractListContent(
        selectedTabIndex = selectedTab.ordinal,
        pagerState = pagerState,
        onTabSelected = { index ->
            scope.launch {
                pagerState.animateScrollToPage(index)
            }
        },
        contracts = contracts,
        paginatedContracts = paginatedContracts,
        currentPage = currentPage,
        totalPages = totalPages,
        hasNextPage = hasNextPage,
        hasPreviousPage = hasPreviousPage,
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage,
        onPageClick = viewModel::goToPage,
        loading = loading,
        error = error,
        ownedBuildings = ownedBuildings,
        onContractClick = onContractClick
    )
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            viewModel.selectTab(ContractTab.entries[pagerState.currentPage])
        }
    }

}

@Composable
fun ContractListContent(
    selectedTabIndex: Int = 0,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit = { _ -> },
    contracts: List<ContractWithRoom> = emptyList(),
    paginatedContracts: List<ContractWithRoom> = emptyList(),
    currentPage: Int = 0,
    totalPages: Int = 0,
    hasNextPage: Boolean = false,
    hasPreviousPage: Boolean = false,
    onNextPage: () -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onPageClick: (Int) -> Unit = {},
    loading: Boolean = false,
    error: String? = null,
    ownedBuildings: List<Building> = emptyList(),
    onContractClick: (String) -> Unit = {}
) {
    var selectedBuildingId by remember { mutableStateOf<String?>(null) }
    var showNoBuildingsDialog by remember { mutableStateOf(false) }

    // Check if user has no buildings and show dialog
    LaunchedEffect(ownedBuildings) {
        if (ownedBuildings.isEmpty() && !loading) {
            showNoBuildingsDialog = true
        }
    }

    // Add "All" option to the building options
    val buildingOptions = if (ownedBuildings.isNotEmpty()) {
        listOf("" to "All") + ownedBuildings.map { it.id to it.name }
    } else {
        listOf("" to "No buildings available")
    }

    // Filter contracts based on selected building (null/empty means show all)
    val filteredContracts = if (selectedBuildingId.isNullOrEmpty()) {
        paginatedContracts
    } else {
        paginatedContracts.filter { it.contract.buildingId == selectedBuildingId }
    }

    if (showNoBuildingsDialog) {
        AlertDialog(
            onDismissRequest = { showNoBuildingsDialog = false },
            title = { Text("No buildings found") },
            text = { Text("You don't have any buildings registered. Please add a building first to create contracts.") },
            confirmButton = {
                TextButton(onClick = { showNoBuildingsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Tabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
                tabData = tabData
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                DropdownSelectField(
                    label = "Filter by Building",
                    options = buildingOptions.map { it.second },
                    selectedOption = buildingOptions.find { it.first == selectedBuildingId }?.second
                        ?: buildingOptions.firstOrNull()?.second,
                    onOptionSelected = { name ->
                        selectedBuildingId = buildingOptions.find { it.second == name }?.first
                    },
                    enabled = ownedBuildings.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    HorizontalPager(
                        state = pagerState,
                        key = { page -> tabData[page].first }
                    ) { page ->
                        if (page == selectedTabIndex) {
                            ContractListPage(
                                isLoading = loading,
                                contracts = if (ownedBuildings.isEmpty()) emptyList() else filteredContracts,
                                emptyMessage = if (ownedBuildings.isEmpty()) {
                                    "No buildings found. Please add a building first."
                                } else {
                                    "No ${tabData[page].first.lowercase()} contracts found."
                                },
                                onContractClick = onContractClick
                            )
                            
                            // Add pagination controls
                            if (totalPages > 1 && !loading && error == null && ownedBuildings.isNotEmpty()) {
                                PaginationControls(
                                    currentPage = currentPage,
                                    totalPages = totalPages,
                                    hasNextPage = hasNextPage,
                                    hasPreviousPage = hasPreviousPage,
                                    onNextPage = onNextPage,
                                    onPreviousPage = onPreviousPage,
                                    onPageClick = onPageClick,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                    if (error != null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Error: $error")
                        }
                    }
                }
            }
        }
    )
}


@Composable
fun ContractListPage(
    isLoading: Boolean,
    contracts: List<ContractWithRoom>,
    emptyMessage: String,
    onContractClick: (String) -> Unit = {}
) {
    Crossfade(targetState = isLoading, label = "Page Loading Crossfade") { loadingState ->
        if (loadingState) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ContractList(contracts = contracts, emptyMessage = emptyMessage, onContractClick = onContractClick)
        }
    }
}

@Composable
fun ContractList(
    contracts: List<ContractWithRoom>,
    emptyMessage: String,
    isLoading: Boolean = false,
    onContractClick: (String) -> Unit = {}
) {
    if (!isLoading && contracts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = emptyMessage)
        }
    } else if (!isLoading) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contracts) { contractWithRoom ->
                ContractListItem(
                    contractWithRoom = contractWithRoom,
                    onClick = {
                        Log.d("NavigationCheck", "Navigating with contractId: '${contractWithRoom.contract.id}'")
                        onContractClick(contractWithRoom.contract.id)
                    }
                )
            }
        }
    }
}

@Composable
fun ContractListItem(
    contractWithRoom: ContractWithRoom,
    onClick: () -> Unit = {},
) {
    val contract = contractWithRoom.contract
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Contract number: ${contract.contractNumber}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Room: ${contractWithRoom.roomNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Start: ${contract.startDate} - End: ${contract.endDate}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}