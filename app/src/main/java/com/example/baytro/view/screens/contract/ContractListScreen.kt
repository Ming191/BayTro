package com.example.baytro.view.screens.contract

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.baytro.view.components.CompactSearchBar
import com.example.baytro.view.components.ContractListSkeleton
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.Tabs
import com.example.baytro.viewModel.contract.ContractListVM
import com.example.baytro.viewModel.contract.ContractTab
import com.example.baytro.viewModel.contract.ContractWithRoom
import kotlinx.coroutines.delay
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
    val filteredContracts by viewModel.filteredContracts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val ownedBuildings by viewModel.ownedBuildings.collectAsState()
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal, pageCount = { tabData.size })
    val scope = rememberCoroutineScope()

    var hasLoadedOnce by remember { mutableStateOf(false) }

    Log.d("ContractListScreen", "Recomposing - loading=$loading, hasLoadedOnce=$hasLoadedOnce, contractsCount=${filteredContracts.size}")

    LaunchedEffect(loading) {
        if (!loading && !hasLoadedOnce) {
            Log.d("ContractListScreen", "First load complete, transitioning to content")
            delay(300)
            hasLoadedOnce = true
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            viewModel.selectTab(ContractTab.entries[pagerState.currentPage])
        }
    }

    if (!hasLoadedOnce && loading) {
        Log.d("ContractListScreen", "Rendering skeleton loading")
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    Tabs(
                        selectedTabIndex = selectedTab.ordinal,
                        onTabSelected = {},
                        tabData = tabData
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    ContractListSkeleton(itemCount = 5)
                }
            }
        }
    } else {
        // Show actual content after first load
        Log.d("ContractListScreen", "Rendering content with ${filteredContracts.size} contracts")
        ContractListContent(
            viewModel = viewModel,
            selectedTabIndex = selectedTab.ordinal,
            pagerState = pagerState,
            onTabSelected = { index ->
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            ownedBuildings = ownedBuildings,
            onContractClick = onContractClick
        )
    }

}

@Composable
private fun ContractListContent(
    viewModel: ContractListVM,
    selectedTabIndex: Int,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    ownedBuildings: List<Building>,
    onContractClick: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredContracts by viewModel.filteredContracts.collectAsState()
    val selectedBuildingId by viewModel.selectedBuildingId.collectAsState()
    val error by viewModel.error.collectAsState()

    var showNoBuildingsDialog by remember { mutableStateOf(false) }
    val animatedItemIds = remember { mutableSetOf<String>() }

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        visible = true
    }

    LaunchedEffect(selectedTabIndex) {
        animatedItemIds.clear()
    }

    LaunchedEffect(ownedBuildings) {
        if (ownedBuildings.isEmpty()) {
            showNoBuildingsDialog = true
        }
    }

    val buildingOptions = if (ownedBuildings.isNotEmpty()) {
        listOf("" to "All") + ownedBuildings.map { it.id to it.name }
    } else {
        listOf("" to "No buildings available")
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar with animation
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { -it }
                ),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { -it }
                )
            ) {
                CompactSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholderText = "Search contracts..."
                )
            }

            // Building filter dropdown with animation
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) + slideInVertically(
                    animationSpec = tween(300, delayMillis = 100),
                    initialOffsetY = { -it / 2 }
                ),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { -it / 2 }
                )
            ) {
                DropdownSelectField(
                    label = "Filter by Building",
                    options = buildingOptions.map { it.second },
                    selectedOption = buildingOptions.find { it.first == selectedBuildingId }?.second
                        ?: buildingOptions.firstOrNull()?.second,
                    onOptionSelected = { name ->
                        val newBuildingId = buildingOptions.find { it.second == name }?.first
                        viewModel.setSelectedBuildingId(newBuildingId)
                    },
                    enabled = ownedBuildings.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    key = { page -> tabData[page].first }
                ) { page ->
                    if (page == selectedTabIndex) {
                        ContractListPage(
                            contracts = if (ownedBuildings.isEmpty()) emptyList() else filteredContracts,
                            emptyMessage = if (ownedBuildings.isEmpty()) {
                                "No buildings found. Please add a building first."
                            } else {
                                "No ${tabData[page].first.lowercase()} contracts found."
                            },
                            animatedItemIds = animatedItemIds,
                            onContractClick = onContractClick
                        )
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
}

@Composable
private fun ContractListPage(
    contracts: List<ContractWithRoom>,
    emptyMessage: String,
    animatedItemIds: MutableSet<String>,
    onContractClick: (String) -> Unit
) {
    if (contracts.isEmpty()) {
        var emptyStateVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            Log.d("ContractList", "Empty state animation")
            emptyStateVisible = false
            delay(100)
            emptyStateVisible = true
        }

        AnimatedVisibility(
            visible = emptyStateVisible,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = emptyMessage)
            }
        }
    } else {
        var isInitialLoad by remember { mutableStateOf(true) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = contracts,
                key = { _, item -> item.contract.id }
            ) { index, contractWithRoom ->
                val itemId = contractWithRoom.contract.id
                var visible by remember(itemId) {
                    mutableStateOf(animatedItemIds.contains(itemId))
                }

                LaunchedEffect(itemId) {
                    if (!visible) {
                        // Only delay for initial load
                        if (isInitialLoad) {
                            delay(50)
                        }
                        visible = true
                        animatedItemIds.add(itemId)
                    }
                }

                LaunchedEffect(Unit) {
                    if (isInitialLoad && index >= 2) {
                        isInitialLoad = false
                    }
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialAlpha = 0f
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { it / 3 }
                    )
                ) {
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
}

@Composable
private fun ContractListItem(
    contractWithRoom: ContractWithRoom,
    onClick: () -> Unit
) {
    val contract = contractWithRoom.contract
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Contract number: ${contract.contractNumber}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Room: ${contractWithRoom.roomNumber}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Start: ${contract.startDate} - End: ${contract.endDate}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}