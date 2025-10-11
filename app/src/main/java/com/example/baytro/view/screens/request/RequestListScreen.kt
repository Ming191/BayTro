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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.baytro.data.Building
import com.example.baytro.data.request.FullRequestInfo
import com.example.baytro.data.request.Request
import com.example.baytro.data.request.RequestStatus
import com.example.baytro.view.components.CarouselOrientation
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequestListSkeleton
import com.example.baytro.view.components.Tabs
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.request.FilteredRequestData
import com.example.baytro.viewModel.request.RequestListFormState
import com.example.baytro.viewModel.request.RequestListVM
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

    val uiState by viewModel.requestListUiState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    var hasLoadedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success && !hasLoadedOnce) {
            hasLoadedOnce = true
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

    if (!hasLoadedOnce && uiState is UiState.Loading) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    RequestListTopBar(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = {},
                        isTabSelectionEnabled = false
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    RequestListSkeleton(itemCount = 5)
                }
            }
        }
    } else {
        RequestListContent(
            viewModel = viewModel,
            formState = formState,
            selectedTabIndex = selectedTabIndex,
            pagerState = pagerState,
            onTabSelected = onTabSelected,
            uiState = uiState,
            onAddRequest = onAddRequest,
            onAssignRequest = onAssignRequest,
            onUpdateRequest = onUpdateRequest
        )
    }
}

@Composable
private fun RequestListContent(
    viewModel: RequestListVM,
    formState: RequestListFormState,
    selectedTabIndex: Int,
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    uiState: UiState<FilteredRequestData>,
    onAddRequest: () -> Unit,
    onAssignRequest: (String) -> Unit,
    onUpdateRequest: (String) -> Unit
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var isSpeedDialOpen by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        FilterDialog(
            buildings = formState.availableBuildings,
            selectedBuilding = formState.selectedBuilding,
            onBuildingSelected = viewModel::onBuildingChange,
            onDismiss = { showFilterDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Tabs(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { if (uiState is UiState.Success) onTabSelected(it) },
                    tabData = tabData
                )
            },
            floatingActionButton = {}
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (val state = uiState) {
                    is UiState.Loading -> RequestListSkeleton(itemCount = 5)
                    is UiState.Success -> RequestListPager(
                        pagerState = pagerState,
                        requestData = state.data,
                        isLandlord = formState.isLandlord,
                        selectedTabIndex = selectedTabIndex,
                        onAssignRequest = onAssignRequest,
                        viewModel = viewModel,
                        onUpdateRequest = onUpdateRequest
                    )
                    is UiState.Error -> ErrorStateContent(
                        message = state.message,
                        onRetry = viewModel::refreshRequests
                    )
                    else -> { /* No-op */ }
                }
            }
        }

        if (isSpeedDialOpen) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isSpeedDialOpen = false }
                    ),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
            ) {}
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            if (uiState is UiState.Success) {
                val speedDialItems = if (formState.isLandlord) {
                    listOfNotNull(
                        if (formState.availableBuildings.isNotEmpty()) {
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
private fun ErrorStateContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun RequestListPager(
    pagerState: PagerState,
    requestData: FilteredRequestData,
    isLandlord: Boolean,
    selectedTabIndex: Int,
    onAssignRequest: (String) -> Unit,
    viewModel: RequestListVM,
    onUpdateRequest: (String) -> Unit
) {
    HorizontalPager(state = pagerState, key = { page -> tabData[page].first }) { page ->
        val (requestsForPage, statusText) = when (page) {
            0 -> requestData.pending to tabData[0].first.lowercase()
            1 -> requestData.inProgress to tabData[1].first.lowercase()
            2 -> requestData.done to tabData[2].first.lowercase()
            else -> emptyList<FullRequestInfo>() to "requests"
        }

        if (page == selectedTabIndex) {
            RequestListPage(
                requests = requestsForPage,
                emptyMessage = "No $statusText requests found.",
                isLandlord = isLandlord,
                onAssignRequest = onAssignRequest,
                viewModel = viewModel,
                onUpdateRequest = onUpdateRequest
            )
        } else {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun RequestListPage(
    requests: List<FullRequestInfo>,
    emptyMessage: String,
    isLandlord: Boolean,
    onAssignRequest: (String) -> Unit,
    viewModel: RequestListVM,
    onUpdateRequest: (String) -> Unit
) {
    val animatedItemIds = remember { mutableSetOf<String>() }
    var emptyStateVisible by remember { mutableStateOf(false) }

    if (requests.isEmpty()) {
        LaunchedEffect(Unit) {
            emptyStateVisible = true
        }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(items = requests, key = { _, item -> item.request.id }) { index, info ->
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
                        onCompleteRequest = viewModel::completeRequest,
                        onUpdateRequest = onUpdateRequest
                    )
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
    onCompleteRequest: (String) -> Unit,
    onUpdateRequest: (String) -> Unit = {}
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
            // Dải màu chỉ báo trạng thái
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
                            text = "Created: ${request.createdAt}",
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
                    onCompleteRequest = onCompleteRequest,
                    onUpdateRequest = onUpdateRequest
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
        // Thông tin người yêu cầu và vị trí
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

        // Ngày hẹn
        if (request.scheduledDate.isNotBlank()) {
            DetailRow(
                icon = Icons.Filled.DateRange,
                label = "Scheduled for",
                value = request.scheduledDate
            )
        }

        // Thông tin theo từng trạng thái
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
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
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
    onCompleteRequest: (String) -> Unit,
    onUpdateRequest: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showContactDialog by remember { mutableStateOf(false) }
    var isCompletingRequest by remember { mutableStateOf(false) }

    // Contact dialog for tenant in IN_PROGRESS status (choice between landlord and assignee)
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
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = "tel:${info.landlordPhoneNumber}".toUri()
                        }
                        context.startActivity(intent)
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
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = "tel:$phone".toUri()
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Assignee")
                    }
                }
            }
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Contact button logic
        OutlinedButton(
            onClick = {
                if (!isLandlord && request.status == RequestStatus.IN_PROGRESS) {
                    // Tenant in IN_PROGRESS: show dialog to choose between landlord and assignee
                    showContactDialog = true
                } else {
                    // Other cases: direct call
                    val phoneNumber = if (isLandlord) {
                        info.tenantPhoneNumber
                    } else {
                        info.landlordPhoneNumber
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = "tel:$phoneNumber".toUri()
                    }
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            enabled = !isCompletingRequest
        ) {
            Text("Contact", style = MaterialTheme.typography.labelLarge)
        }

        // Action buttons
        when {
            // Landlord PENDING: show Assign button
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
            // Tenant IN_PROGRESS: show Complete button
            !isLandlord && request.status == RequestStatus.IN_PROGRESS -> {
                Button(
                    onClick = {
                        isCompletingRequest = true
                        onCompleteRequest(request.id)
                        android.widget.Toast.makeText(
                            context,
                            "Request marked as completed",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    enabled = !isCompletingRequest
                ) {
                    if (isCompletingRequest) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    } else {
                        Text("Complete", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            // Tenant PENDING: show Update button
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
    buildings: List<Building>,
    selectedBuilding: Building?,
    onBuildingSelected: (Building) -> Unit,
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

                DropdownSelectField(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Select Building",
                    options = buildings,
                    selectedOption = selectedBuilding,
                    onOptionSelected = onBuildingSelected,
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
