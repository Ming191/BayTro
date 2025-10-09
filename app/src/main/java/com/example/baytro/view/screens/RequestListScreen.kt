package com.example.baytro.view.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.Request.Request
import com.example.baytro.data.Request.RequestStatus
import com.example.baytro.ui.theme.AppTheme
import com.example.baytro.view.components.ContractInfoRow
import com.example.baytro.view.components.RequestListSkeleton
import com.example.baytro.view.components.Tabs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val sampleRequests = listOf(
    Request(
        id = "1",
        title = "Broken bath",
        description = "My bath is leaking---",
        status = RequestStatus.PENDING,
        createdAt = "18/09/2025",
        scheduledDate = "20/09/2025",
        imageUrls = listOf(
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT9lcsxrF8y6syCvTXgZXwX6M1Bkdm0Q189rQ&s",
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT9lcsxrF8y6syCvTXgZXwX6M1Bkdm0Q189rQ&s",
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT9lcsxrF8y6syCvTXgZXwX6M1Bkdm0Q189rQ&s"),
    ),
    Request(
        id = "2",
        title = "Air conditioner not working",
        description = "The AC in the bedroom is not cooling properly",
        status = RequestStatus.IN_PROGRESS,
        createdAt = "15/09/2025",
        scheduledDate = "22/09/2025",
        imageUrls = listOf("", ""),
    ),
    Request(
        id = "3",
        title = "Light bulb replacement",
        description = "Kitchen light needs replacement",
        status = RequestStatus.DONE,
        createdAt = "10/09/2025",
        scheduledDate = "12/09/2025",
        imageUrls = listOf(""),
    ),
)

private val tabData: List<Pair<String, ImageVector>> = listOf(
    "Pending" to Icons.Filled.HourglassEmpty,
    "In Progress" to Icons.Filled.Schedule,
    "Done" to Icons.Filled.CheckCircle
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabData.size })
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<UiState<List<Request>>>(UiState.Loading) }

    LaunchedEffect(Unit) {
        delay(1500)
        uiState = UiState.Success(sampleRequests)
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            Scaffold(
                topBar = {
                    Tabs(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = {},
                        tabData = tabData
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    RequestListSkeleton(itemCount = 5)
                }
            }
        }
        is UiState.Success -> {
            Scaffold(
                topBar = {
                    Tabs(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = { index ->
                            scope.launch {
                                selectedTabIndex = index
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        tabData = tabData
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { /* TODO: Navigate to request form */ },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Request")
                    }
                }
            ) { paddingValues ->
                RequestListContent(
                    paddingValues = paddingValues,
                    pagerState = pagerState,
                    selectedTabIndex = selectedTabIndex,
                    requests = state.data
                )
            }
        }
        else -> {}
    }
}

@Composable
private fun RequestListContent(
    paddingValues: PaddingValues,
    pagerState: PagerState,
    selectedTabIndex: Int,
    requests: List<Request>
) {
    val animatedItemIds = remember(selectedTabIndex) { mutableSetOf<String>() }

    LaunchedEffect(selectedTabIndex) {
        animatedItemIds.clear()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> tabData[page].first }
        ) { page ->
            val filteredRequests = when (page) {
                0 -> requests.filter { it.status == RequestStatus.PENDING }
                1 -> requests.filter { it.status == RequestStatus.IN_PROGRESS }
                2 -> requests.filter { it.status == RequestStatus.DONE }
                else -> emptyList()
            }

            RequestListPage(
                requests = filteredRequests,
                emptyMessage = "No ${tabData[page].first.lowercase()} requests found.",
                animatedItemIds = animatedItemIds
            )
        }
    }
}

@Composable
private fun RequestListPage(
    requests: List<Request>,
    emptyMessage: String,
    animatedItemIds: MutableSet<String>
) {
    var emptyStateVisible by remember { mutableStateOf(false) }

    if (requests.isEmpty()) {
        LaunchedEffect(Unit) {
            delay(50)
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = requests,
                key = { _, item -> item.id }
            ) { index, request ->
                val itemId = request.id
                var visible by remember(itemId) {
                    mutableStateOf(animatedItemIds.contains(itemId))
                }

                LaunchedEffect(itemId) {
                    if (!visible) {
                        delay(50L * index.coerceAtMost(10))
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
                    RequestCard(request = request)
                }
            }
        }
    }
}


@Composable
fun RequestCard(request: Request) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Navigate to detail */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RequestCardHeader(request)

            Spacer(modifier = Modifier.height(12.dp))

            RequestCardDetails(request)

            if (request.imageUrls.isNotEmpty() && request.imageUrls.any { it.isNotEmpty() }) {
                Spacer(modifier = Modifier.height(12.dp))
                RequestCardImages(request.imageUrls)
            }

            Spacer(modifier = Modifier.height(16.dp))

            RequestCardActions()
        }
    }
}

@Composable
fun RequestCardHeader(request: Request) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = request.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = request.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RequestCardDetails(request: Request) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val infoMap = mapOf(
            "Contract ID:" to "123456",
            "Property Address:" to "123 Main St, Springfield",
            "Landlord Name:" to "John Doe",
            "Tenant Name:" to "Jane Smith",
            "Start Date:" to "2023-01-01",
            "End Date:" to "2023-12-31",
            "Rent Amount:" to "$1200/month"
        )
        Column {
            for (entry in infoMap) {
                ContractInfoRow(label = entry.key, value = entry.value)
            }
        }
    }
}

@Composable
fun RequestCardImages(imageUrls: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Attached Images",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ReadOnlyPhotoCarousel(imageUrls = imageUrls)
    }
}

@Composable
fun ReadOnlyPhotoCarousel(imageUrls: List<String>) {
    // Display images in a horizontal scrollable row without edit capability
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageUrls.take(3).forEach { imageUrl ->
            Card(
                modifier = Modifier
                    .size(width = 100.dp, height = 100.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotEmpty()) {
                        coil3.compose.AsyncImage(
                            model = imageUrl,
                            contentDescription = "Request image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        // Placeholder for empty image
                        Text(
                            text = "📷",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (imageUrls.size > 3) {
            Card(
                modifier = Modifier
                    .size(width = 100.dp, height = 100.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${imageUrls.size - 3}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun RequestCardActions() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { /* TODO: Contact tenant */ },
            modifier = Modifier.weight(1f)
        ) {
            Text("Contact")
        }
        Button(
            onClick = { /* TODO: Assign technician */ },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Assign")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun RequestScreenPreview() {
    AppTheme {
        RequestScreen()
    }
}