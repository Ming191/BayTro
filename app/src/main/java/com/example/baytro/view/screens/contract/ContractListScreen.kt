package com.example.baytro.view.screens.contract

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.baytro.data.contract.Status
import com.example.baytro.utils.cloudFunctions.ContractWithRoom
import com.example.baytro.view.components.CompactSearchBar
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
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                ContractListPage(
                    contracts = if (uiState.buildings.isEmpty()) emptyList() else uiState.contracts,
                    emptyMessage = if (uiState.buildings.isEmpty()) {
                        "No buildings found. Please add a building first."
                    } else {
                        "No ${tab.name.lowercase()} contracts found."
                    },
                    onContractClick = onContractClick,
                    loading = uiState.isLoading
                )
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

                        ContractListItem(
                            contractWithRoom = contractWithRoom,
                            onClick = onItemClick
                        )
                    }
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
    val semanticsDescription = remember(contract, contractWithRoom) {
        buildString {
            append("Contract ${contract.contractNumber}, ")
            append("Room ${contractWithRoom.roomNumber}, ")
            append("From ${contract.startDate} to ${contract.endDate}")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = semanticsDescription },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contract.contractNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    StatusChip(status = contract.status)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Room location",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Room ${contractWithRoom.roomNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Contract dates",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${contract.startDate} – ${contract.endDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusChip(status: Status) {
    val backgroundColor: Color
    val textColor: Color
    val label: String
    val icon: ImageVector

    when (status) {
        Status.ACTIVE -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            textColor = MaterialTheme.colorScheme.onPrimaryContainer
            label = "Active"
            icon = Icons.Default.CheckCircle
        }
        Status.PENDING -> {
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer
            textColor = MaterialTheme.colorScheme.onSecondaryContainer
            label = "Pending"
            icon = Icons.Default.Schedule
        }
        Status.OVERDUE -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            textColor = MaterialTheme.colorScheme.onErrorContainer
            label = "Overdue"
            icon = Icons.Default.CalendarToday
        }
        Status.ENDED -> {
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
            label = "Ended"
            icon = Icons.Default.CheckCircle
        }
    }

    Surface(
        modifier = Modifier.semantics {
            contentDescription = "Contract status: $label"
        },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = textColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

