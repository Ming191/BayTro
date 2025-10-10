package com.example.baytro.view.screens.request

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
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
    onAddRequest: () -> Unit
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
                        formState = formState,
                        selectedTabIndex = selectedTabIndex,
                        onBuildingSelected = {},
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
            onAddRequest = onAddRequest
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
    onAddRequest: () -> Unit
) {
    Scaffold(
        topBar = {
            RequestListTopBar(
                formState = formState,
                selectedTabIndex = selectedTabIndex,
                onBuildingSelected = viewModel::onBuildingChange,
                onTabSelected = onTabSelected,
                isTabSelectionEnabled = uiState is UiState.Success
            )
        },
        floatingActionButton = {
            if (uiState is UiState.Success) {
                FloatingActionButton(
                    onClick = onAddRequest,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Request")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is UiState.Loading -> RequestListSkeleton(itemCount = 5)
                is UiState.Success -> RequestListPager(
                    pagerState = pagerState,
                    requestData = state.data,
                    isLandlord = formState.isLandlord,
                    selectedTabIndex = selectedTabIndex
                )
                is UiState.Error -> ErrorStateContent(
                    message = state.message,
                    onRetry = viewModel::refreshRequests
                )
                else -> { /* No-op */}
            }
        }
    }
}

@Composable
private fun RequestListTopBar(
    formState: RequestListFormState,
    selectedTabIndex: Int,
    onBuildingSelected: (Building) -> Unit,
    onTabSelected: (Int) -> Unit,
    isTabSelectionEnabled: Boolean
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        Tabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { if (isTabSelectionEnabled) onTabSelected(it) },
            tabData = tabData
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (formState.isLandlord && formState.availableBuildings.isNotEmpty()) {
                DropdownSelectField(
                    label = "Building",
                    options = formState.availableBuildings,
                    selectedOption = formState.selectedBuilding,
                    onOptionSelected = onBuildingSelected,
                    optionToString = { it.name },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
    selectedTabIndex: Int
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
                isLandlord = isLandlord
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
    isLandlord: Boolean
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
                    RequestCard(info = info, isLandlord = isLandlord)
                }
            }
        }
    }
}

@Composable
fun RequestCard(info: FullRequestInfo, isLandlord: Boolean) {
    val request = info.request
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.request.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                StatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = info.request.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details section
            RequestCardDetails(info)

            val validImageUrls = request.imageUrls.filter { it.isNotEmpty() }
            if (validImageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                RequestCardImages(imageUrls = validImageUrls)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            RequestCardActions(request = request, isLandlord = isLandlord)
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
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun RequestCardDetails(info: FullRequestInfo) {
    val request = info.request
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Using icons for better visual appeal
            DetailRow(
                icon = Icons.Filled.Schedule,
                label = "Created",
                value = request.createdAt
            )
            DetailRow(
                icon = Icons.Filled.Schedule,
                label = "Scheduled",
                value = request.scheduledDate
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Room ${info.roomName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = info.buildingName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = info.tenantName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Requester",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Phase-specific fields
            when (request.status) {
                RequestStatus.PENDING -> {
                    // Pending: Only show basic info (already shown above)
                }
                RequestStatus.IN_PROGRESS -> {
                    request.assigneeName?.let { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Assigned to",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            request.assigneePhoneNumber?.let { phone ->
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                RequestStatus.DONE -> {
                    request.assigneeName?.let { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Completed by",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            request.assigneePhoneNumber?.let { phone ->
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        request.completionDate?.let { date ->
                            DetailRow(
                                icon = Icons.Filled.CheckCircle,
                                label = "Completed on",
                                value = date
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
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
fun RequestCardActions(request: Request, isLandlord: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { /* TODO: Contact */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Contact", style = MaterialTheme.typography.labelLarge)
        }

        // Only show action buttons for landlords
        if (isLandlord) {
            when (request.status) {
                RequestStatus.PENDING -> {
                    Button(
                        onClick = { /* TODO: Update status */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Assign", style = MaterialTheme.typography.labelLarge)
                    }
                }
                RequestStatus.IN_PROGRESS -> {
                    Button(
                        onClick = { /* TODO: Update status */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("Complete", style = MaterialTheme.typography.labelLarge)
                    }
                }
                RequestStatus.DONE -> {
                    // No additional button for completed requests
                }
            }
        }
    }
}
