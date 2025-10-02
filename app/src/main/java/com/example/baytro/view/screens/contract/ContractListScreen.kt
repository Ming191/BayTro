package com.example.baytro.view.screens.contract

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.baytro.data.building.Building
import com.example.baytro.data.contract.Contract
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.Tabs
import com.example.baytro.viewModel.contract.ContractListVM
import com.example.baytro.viewModel.contract.ContractTab
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val tabData: List<Pair<String, ImageVector>> = listOf(
    "Active" to Icons.Filled.HomeWork,
    "Pending" to Icons.Filled.Schedule,
    "Ended" to Icons.Filled.CheckCircle
)

@Composable
fun ContractListScreen(
    viewModel: ContractListVM = koinViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val ownedBuildings by viewModel.ownedBuildings.collectAsState()
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
        loading = loading,
        error = error,
        ownedBuildings = ownedBuildings
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
    contracts: List<Contract> = emptyList(),
    loading: Boolean = false,
    error: String? = null,
    ownedBuildings: List<Building> = emptyList()
) {
    var selectedBuildingId by remember { mutableStateOf<String?>(null) }
    val buildingOptions = ownedBuildings.map { it.id to it.name }
    val filteredContracts = selectedBuildingId?.let { id ->
        contracts.filter { it.buildingId == id }
    } ?: contracts
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
                if (buildingOptions.isNotEmpty()) {
                    DropdownSelectField(
                        label = "Filter by Building",
                        options = buildingOptions.map { it.second },
                        selectedOption = buildingOptions.find { it.first == selectedBuildingId }?.second,
                        onOptionSelected = { name ->
                            selectedBuildingId = buildingOptions.find { it.second == name }?.first
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    HorizontalPager(
                        state = pagerState,
                        key = { page -> tabData[page].first }
                    ) { page ->
                        if (page == selectedTabIndex) {
                            ContractListPage(
                                isLoading = loading,
                                contracts = filteredContracts,
                                emptyMessage = "No ${tabData[page].first.lowercase()} contracts found."
                            )
                        } else {
                            // For non-selected tabs, show nothing
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
    contracts: List<Contract>,
    emptyMessage: String
) {
    Crossfade(targetState = isLoading, label = "Page Loading Crossfade") { loadingState ->
        if (loadingState) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ContractList(contracts = contracts, emptyMessage = emptyMessage)
        }
    }
}

@Composable
fun ContractList(
    contracts: List<Contract>,
    emptyMessage: String,
    isLoading: Boolean = false,
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
            items(contracts) { contract ->
                ContractListItem(contract = contract)
            }
        }
    }
}

@Composable
fun ContractListItem(contract: Contract) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Navigate to contract details */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Room: ${contract.roomNumber}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Start: ${contract.startDate} - End: ${contract.endDate}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}