package com.example.baytro.view.screens.contract

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.baytro.utils.cloudFunctions.ContractWithRoom
import com.example.baytro.view.components.CompactSearchBar
import com.example.baytro.view.components.ContractCard
import com.example.baytro.view.components.ContractListSkeleton
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.Tabs
import com.example.baytro.viewModel.contract.ContractListUiState
import com.example.baytro.viewModel.contract.ContractListVM
import com.example.baytro.viewModel.contract.ContractTab
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private enum class LoadingState {
    LOADING, CONTENT, EMPTY
}

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
    val uiState by viewModel.uiState.collectAsState()

    ContractListContent(
        viewModel = viewModel,
        uiState = uiState,
        onTabSelected = { index ->
            viewModel.selectTab(ContractTab.entries[index])
        },
        onContractClick = onContractClick
    )
}

@Composable
private fun ContractListContent(
    viewModel: ContractListVM,
    uiState: ContractListUiState,
    onTabSelected: (Int) -> Unit,
    onContractClick: (String) -> Unit
) {
    var showNoBuildingsDialog by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Only show dialog when not loading AND buildings are empty
    LaunchedEffect(uiState.buildings, uiState.isLoading) {
        if (!uiState.isLoading && uiState.buildings.isEmpty()) {
            showNoBuildingsDialog = true
        } else if (uiState.buildings.isNotEmpty()) {
            showNoBuildingsDialog = false
        }
    }

    val buildingOptions = remember(uiState.buildings) {
        if (uiState.buildings.isNotEmpty()) {
            listOf("" to "All") + uiState.buildings.map { it.id to it.name }
        } else {
            listOf("" to "No buildings available")
        }
    }

    val selectedOption by remember(uiState.selectedBuildingId, buildingOptions) {
        derivedStateOf {
            buildingOptions.find { it.first == uiState.selectedBuildingId }?.second
                ?: buildingOptions.firstOrNull()?.second
        }
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

    val pagerState = rememberPagerState(
        initialPage = uiState.selectedTab.ordinal,
        pageCount = { ContractTab.entries.size }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != uiState.selectedTab.ordinal) {
            onTabSelected(pagerState.currentPage)
        }
    }

    val onTabClicked: (Int) -> Unit = { index ->
        coroutineScope.launch {
            pagerState.animateScrollToPage(index)
        }
        onTabSelected(index)
    }

    Scaffold(
        topBar = {
            Tabs(
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = onTabClicked,
                tabData = tabData
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                CompactSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "Search contracts by contract number or room details"
                        },
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholderText = "Search contracts..."
                )

                DropdownSelectField(
                    label = "Filter by Building",
                    options = buildingOptions.map { it.second },
                    selectedOption = selectedOption,
                    onOptionSelected = { name ->
                        val newBuildingId = buildingOptions.find { it.second == name }?.first
                        viewModel.setSelectedBuildingId(newBuildingId)
                    },
                    enabled = uiState.buildings.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "Filter contracts by building. Currently selected: ${selectedOption ?: "All"}"
                        }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val tab = ContractTab.entries[page]
                    val contractsToShow = uiState.contractsByTab[tab] ?: emptyList()
                    val isLoadingForThisTab = (uiState.isLoading || uiState.isRefreshing) && uiState.selectedTab == tab

                    ContractListPage(
                        contracts = contractsToShow,
                        emptyMessage = if (uiState.buildings.isEmpty()) {
                            "No buildings found. Please add a building first."
                        } else {
                            "No ${tab.name.lowercase()} contracts found."
                        },
                        onContractClick = onContractClick,
                        loading = isLoadingForThisTab
                    )
                }
            }
        }
    }
}

@Composable
private fun ContractListPage(
    contracts: List<ContractWithRoom>,
    emptyMessage: String,
    onContractClick: (String) -> Unit,
    loading: Boolean = false
) {
    val loadingState by remember(loading, contracts) {
        derivedStateOf {
            when {
                loading -> LoadingState.LOADING
                contracts.isNotEmpty() -> LoadingState.CONTENT
                else -> LoadingState.EMPTY
            }
        }
    }

    Crossfade(
        targetState = loadingState,
        animationSpec = tween(durationMillis = 300),
        label = "Contract list state crossfade"
    ) { state ->
        when (state) {
            LoadingState.LOADING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Loading contracts" }
                ) {
                    ContractListSkeleton(itemCount = 5)
                }
            }
            LoadingState.EMPTY -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = emptyMessage },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LoadingState.CONTENT -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Contracts list" },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = contracts.size,
                        key = { index -> contracts[index].contract.id }
                    ) { index ->
                        val contractWithRoom = contracts[index]
                        val itemId = contractWithRoom.contract.id
                        val onItemClick = remember(itemId) {
                            {
                                if (itemId.isNotBlank()) {
                                    onContractClick(itemId)
                                }
                            }
                        }
                        ContractCard(
                            contractNumber = contractWithRoom.contract.contractNumber,
                            roomNumber = contractWithRoom.roomNumber,
                            startDate = contractWithRoom.contract.startDate,
                            endDate = contractWithRoom.contract.endDate,
                            status = contractWithRoom.contract.status,
                            onClick = onItemClick
                        )
                    }
                }
            }
        }
    }
}

