package com.example.baytro.view.screens.request

import SpeedDialFab
import SpeedDialMenuItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.baytro.data.BuildingSummary
import com.example.baytro.data.request.FullRequestInfo
import com.example.baytro.data.request.Request
import com.example.baytro.data.request.RequestStatus
import com.example.baytro.utils.Utils
import com.example.baytro.view.components.CarouselOrientation
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequestListSkeleton
import com.example.baytro.view.components.Tabs
import com.example.baytro.viewModel.request.RequestListVM
import com.example.baytro.viewModel.request.RequestListUiState
import com.example.baytro.viewModel.request.CategorizedRequests
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


private val tabData: List<Pair<String, ImageVector>> = listOf(
    "Pending" to Icons.Filled.HourglassEmpty,
    "In Progress" to Icons.Filled.Schedule,
    "Done" to Icons.Filled.CheckCircle
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    viewModel: RequestListVM = koinViewModel(),
    onAddRequest: () -> Unit,
    onAssignRequest: (String) -> Unit = {},
    onUpdateRequest: (String) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabData.size })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()

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

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    val onTabSelected: (Int) -> Unit = { index ->
        scope.launch {
            selectedTabIndex = index
            pagerState.animateScrollToPage(index)
        }
    }

    if (uiState.isLoading) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    RequestListTopBar(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = {},
                        isTabSelectionEnabled = false
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    RequestListSkeleton(itemCount = 5)
                }
            }
        }
    } else {
        RequestListContent(
            viewModel = viewModel,
            uiState = uiState,
            selectedTabIndex = selectedTabIndex,
            pagerState = pagerState,
            onTabSelected = onTabSelected,
            onAddRequest = onAddRequest,
            onAssignRequest = onAssignRequest,
            onUpdateRequest = onUpdateRequest,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
private fun RequestListContent(
    viewModel: RequestListVM,
    uiState: RequestListUiState,
    selectedTabIndex: Int,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    onAddRequest: () -> Unit,
    onAssignRequest: (String) -> Unit,
    onUpdateRequest: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var isSpeedDialOpen by remember { mutableStateOf(false) }

    val lazyListStates = remember {
        mapOf(
            0 to LazyListState(),
            1 to LazyListState(),
            2 to LazyListState()
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            buildings = uiState.buildings,
            selectedBuildingId = uiState.selectedBuildingId,
            onBuildingSelected = viewModel::selectBuilding,
            onDismiss = { showFilterDialog = false }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val speedDialItems = if (uiState.isLandlord) {
                listOfNotNull(
                    if (uiState.buildings.isNotEmpty()) {
                        SpeedDialMenuItem(
                            icon = Icons.Default.FilterList,
                            label = "Filter"
                        )
                    } else null
                )
            } else {
                listOf(
                    SpeedDialMenuItem(
                        icon = Icons.Default.Add,
                        label = "New Request"
                    )
                )
            }

            if (speedDialItems.isNotEmpty()) {
                SpeedDialFab(
                    isMenuOpen = isSpeedDialOpen,
                    onToggleMenu = { isSpeedDialOpen = !isSpeedDialOpen },
                    items = speedDialItems,
                    mainIconOpen = Icons.Filled.Menu,
                    onItemClick = { item ->
                        when (item.label) {
                            "Filter" -> showFilterDialog = true
                            "New Request" -> onAddRequest()
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            RequestListPager(
                pagerState = pagerState,
                categorizedRequests = uiState.categorizedRequests,
                isLandlord = uiState.isLandlord,
                selectedTabIndex = selectedTabIndex,
                onAssignRequest = onAssignRequest,
                onUpdateRequest = onUpdateRequest,
                lazyListStates = lazyListStates,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                hasNextPage = uiState.nextCursor != null,
                isLoadingMore = uiState.isLoadingMore
            )
        }
    }
}

@Composable
private fun RequestListTopBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    isTabSelectionEnabled: Boolean
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        Tabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { if (isTabSelectionEnabled) onTabSelected(it) },
            tabData = tabData
        )
    }
}

@Composable
private fun RequestListPager(
    pagerState: PagerState,
    categorizedRequests: CategorizedRequests,
    isLandlord: Boolean,
    selectedTabIndex: Int,
    onAssignRequest: (String) -> Unit,
    onUpdateRequest: (String) -> Unit,
    lazyListStates: Map<Int, LazyListState>,
    snackbarHostState: SnackbarHostState,
    viewModel: RequestListVM,
    hasNextPage: Boolean,
    isLoadingMore: Boolean
) {
    HorizontalPager(state = pagerState, key = { page -> tabData[page].first }) { page ->
        val (requestsForPage, statusText) = when (page) {
            0 -> categorizedRequests.pending to tabData[0].first.lowercase()
            1 -> categorizedRequests.inProgress to tabData[1].first.lowercase()
            2 -> categorizedRequests.done to tabData[2].first.lowercase()
            else -> emptyList<FullRequestInfo>() to "requests"
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (page == selectedTabIndex) Modifier
                    else Modifier.graphicsLayer(alpha = 0f)
                )
        ) {
            RequestListPage(
                requests = requestsForPage,
                emptyMessage = "No $statusText requests found.",
                isLandlord = isLandlord,
                onAssignRequest = onAssignRequest,
                onUpdateRequest = onUpdateRequest,
                lazyListState = lazyListStates[page] ?: rememberLazyListState(),
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                hasNextPage = hasNextPage && page == selectedTabIndex,
                isLoadingMore = isLoadingMore,
                tabIndex = page,
                isVisible = page == selectedTabIndex
            )
        }
    }
}

@Composable
private fun RequestListPage(
    requests: List<FullRequestInfo>,
    emptyMessage: String,
    isLandlord: Boolean,
    onAssignRequest: (String) -> Unit,
    onUpdateRequest: (String) -> Unit,
    lazyListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    viewModel: RequestListVM,
    hasNextPage: Boolean,
    isLoadingMore: Boolean,
    tabIndex: Int,
    isVisible: Boolean
) {
    val animatedItemIds = remember(tabIndex) { mutableSetOf<String>() }
    var emptyStateVisible by remember { mutableStateOf(false) }

    if (requests.isEmpty()) {
        LaunchedEffect(Unit) { emptyStateVisible = true }

        AnimatedVisibility(
            visible = emptyStateVisible,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        LaunchedEffect(lazyListState, isVisible, hasNextPage, isLoadingMore) {
            if (!isVisible) return@LaunchedEffect

            snapshotFlow {
                val layoutInfo = lazyListState.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem?.index
            }.collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    hasNextPage &&
                    !isLoadingMore &&
                    lastVisibleIndex >= requests.size - 3
                ) {
                    viewModel.loadNextPage()
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            state = lazyListState
        ) {
            itemsIndexed(items = requests, key = { _, item -> item.request.id }) { _, info ->
                val itemId = info.request.id
                var visible by remember(itemId) {
                    mutableStateOf(animatedItemIds.contains(itemId))
                }

                LaunchedEffect(itemId) {
                    if (!visible) {
                        visible = true
                        animatedItemIds.add(itemId)
                    }
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    RequestCard(
                        info = info,
                        isLandlord = isLandlord,
                        onAssignRequest = onAssignRequest,
                        onUpdateRequest = onUpdateRequest,
                        snackbarHostState = snackbarHostState
                    )
                }
            }

            // Loading indicator at bottom
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    info: FullRequestInfo,
    isLandlord: Boolean,
    onAssignRequest: (String) -> Unit,
    onUpdateRequest: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    val request = info.request
    val statusColor = when (request.status) {
        RequestStatus.PENDING -> MaterialTheme.colorScheme.error
        RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        RequestStatus.DONE -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Tiêu đề và Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created: ${request.createdAt?.let { Utils.formatTimestamp(it, "dd/MM/yyyy HH:mm") } ?: "N/A"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(status = request.status)
                }

                // Mô tả
                if (request.description.isNotBlank()) {
                    Text(
                        text = request.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                RequestCardDetails(info = info)

                val validImageUrls = request.imageUrls.filter { it.isNotEmpty() }
                if (validImageUrls.isNotEmpty()) {
                    RequestCardImages(imageUrls = validImageUrls)
                }

                RequestCardActions(
                    request = request,
                    isLandlord = isLandlord,
                    onAssignRequest = onAssignRequest,
                    info = info,
                    onUpdateRequest = onUpdateRequest,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: RequestStatus) {
    val (backgroundColor, textColor, statusText) = when (status) {
        RequestStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Pending"
        )
        RequestStatus.IN_PROGRESS -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "In Progress"
        )
        RequestStatus.DONE -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Completed"
        )
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}


@Composable
fun RequestCardDetails(info: FullRequestInfo) {
    val request = info.request

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow(
            icon = Icons.Filled.Person,
            label = "Requester",
            value = info.tenantName
        )
        DetailRow(
            icon = Icons.Filled.LocationOn,
            label = "Location",
            value = "${info.buildingName} - Room ${info.roomName}"
        )

        if (request.scheduledDate.isNotBlank()) {
            DetailRow(
                icon = Icons.Filled.DateRange,
                label = "Scheduled for",
                value = request.scheduledDate
            )
        }

        when (request.status) {
            RequestStatus.IN_PROGRESS -> {
                request.assigneeName?.let { name ->
                    DetailRow(
                        icon = Icons.Filled.Schedule,
                        label = "Assigned to",
                        value = "$name ${request.assigneePhoneNumber?.let { "($it)" } ?: ""}",
                        valueColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            RequestStatus.DONE -> {
                request.assigneeName?.let { name ->
                    DetailRow(
                        icon = Icons.Filled.CheckCircle,
                        label = "Completed by",
                        value = name,
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                }
                request.completionDate?.let { date ->
                    DetailRow(
                        icon = Icons.Filled.DateRange,
                        label = "Completed on",
                        value = date
                    )
                }
            }
            RequestStatus.PENDING -> {}
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun RequestCardImages(imageUrls: List<String>) {
    Column {
        Text(text = "Attached Images", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        PhotoCarousel(
            selectedPhotos = emptyList(),
            onPhotosSelected = {},
            existingImageUrls = imageUrls,
            onExistingImagesChanged = {},
            maxSelectionCount = 0,
            orientation = CarouselOrientation.Horizontal,
            imageWidth = 120.dp,
            imageHeight = 160.dp,
            showDeleteButton = false
        )
    }
}

@Composable
fun RequestCardActions(
    request: Request,
    info: FullRequestInfo,
    isLandlord: Boolean,
    onAssignRequest: (String) -> Unit,
    onUpdateRequest: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showContactDialog by remember { mutableStateOf(false) }

    if (showContactDialog && request.status == RequestStatus.IN_PROGRESS && !isLandlord) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact") },
            text = {
                Column {
                    Text("Who would you like to contact?")
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showContactDialog = false
                        if (info.landlordPhoneNumber.isNotBlank()) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = "tel:${info.landlordPhoneNumber}".toUri()
                            }
                            context.startActivity(intent)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Landlord phone number not available")
                            }
                        }
                    }
                ) {
                    Text("Landlord")
                }
            },
            dismissButton = {
                request.assigneePhoneNumber?.let { phone ->
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showContactDialog = false
                            if (phone.isNotBlank()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = "tel:$phone".toUri()
                                }
                                context.startActivity(intent)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Assignee phone number not available")
                                }
                            }
                        }
                    ) {
                        Text("Assignee")
                    }
                }
            }
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                if (!isLandlord && request.status == RequestStatus.IN_PROGRESS) {
                    showContactDialog = true
                } else {
                    val phoneNumber = if (isLandlord) {
                        info.tenantPhoneNumber
                    } else {
                        info.landlordPhoneNumber
                    }

                    if (phoneNumber.isNotBlank()) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = "tel:$phoneNumber".toUri()
                        }
                        context.startActivity(intent)
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Phone number not available")
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Contact", style = MaterialTheme.typography.labelLarge)
        }

        when {
            isLandlord && request.status == RequestStatus.PENDING -> {
                Button(
                    onClick = { onAssignRequest(request.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Assign", style = MaterialTheme.typography.labelLarge)
                }
            }
            !isLandlord && request.status == RequestStatus.PENDING -> {
                Button(
                    onClick = { onUpdateRequest(request.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Update", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    buildings: List<BuildingSummary>,
    selectedBuildingId: String?,
    onBuildingSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filter Requests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select a building to view its maintenance requests",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                val selectedBuilding = buildings.find { it.id == selectedBuildingId }

                DropdownSelectField(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Select Building",
                    options = buildings,
                    selectedOption = selectedBuilding,
                    onOptionSelected = { onBuildingSelected(it.id) },
                    optionToString = { it.name }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
