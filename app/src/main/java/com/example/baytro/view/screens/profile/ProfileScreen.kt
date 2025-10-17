package com.example.baytro.view.screens.profile

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.yalantis.ucrop.UCrop
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.example.baytro.data.user.User
import com.example.baytro.viewModel.auth.PersonalInformationVM
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.combinedClickable
import android.graphics.Bitmap
import androidx.compose.runtime.mutableIntStateOf
import com.example.baytro.view.components.PhotoSelectorView
import com.example.baytro.view.components.ImageDetailDialog
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PersonalInformationVM = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToSignOut: () -> Unit,
    onNavigateToEditPersonalInformation: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val header by viewModel.headerState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        Log.d("PersonalInformationScreen", "Screen launched - Checking if need to load")
        viewModel.loadPersonalInformation()
        Log.d("EditPersonalInformationScreen", "Screen is loaded")
    }

    if (isLoading) {
        // Skeleton loading
        ProfileSkeleton(modifier = Modifier.fillMaxSize())
    } else {
        // Load xong => hiển thị UI chính
        Scaffold { paddingValues ->
            user?.let {
                var showPhotoSourceDialog by remember { mutableStateOf(false) }
                var showImagePreview by remember { mutableStateOf(false) }
                var showPicker by remember { mutableStateOf(false) }
                var pickerLaunchKey by remember { mutableIntStateOf(0) }
                var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
                var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

                val context = LocalContext.current
                
                // Create temporary file for camera
                val photoFile = remember {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    File(context.cacheDir, "profile_photo_$timeStamp.jpg")
                }
                
                val photoUri = remember {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                }

                // uCrop launcher - handle result for both gallery/camera flows
                val cropLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val resultUri = UCrop.getOutput(result.data!!)
                        resultUri?.let { croppedUri ->
                            viewModel.updateProfileImage(croppedUri)
                        }
                    }
                    pendingCropUri = null
                }

                // Camera launcher - set pending uri then crop via common flow
                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success: Boolean ->
                    if (success) {
                        cameraImageUri = photoUri
                        pendingCropUri = photoUri
                    }
                }

                // Camera permission launcher
                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        cameraLauncher.launch(photoUri)
                    } else {
                        Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
                    }
                }

                PersonalInformationContent(
                    user = it,
                    onNavigateToChangePassword = onNavigateToChangePassword,
                    onNavigateToSignOut = onNavigateToSignOut,
                    onNavigateToEditPersonalInformation = onNavigateToEditPersonalInformation,
                    onAvatarClick = { showImagePreview = true },
                    onAvatarLongClick = { showPhotoSourceDialog = true },
                    headerViewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )

                if (showPhotoSourceDialog) {
                    AlertDialog(
                        onDismissRequest = { showPhotoSourceDialog = false },
                        title = { Text("Add Photo") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ListItem(
                                    headlineContent = { Text("From Gallery") },
                                    leadingContent = { Icon(imageVector = Icons.Outlined.Image, contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        showPhotoSourceDialog = false
                                        pickerLaunchKey++
                                        showPicker = true
                                    },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                                )
                                ListItem(
                                    headlineContent = { Text("Take Photo") },
                                    leadingContent = { Icon(imageVector = Icons.Outlined.CameraAlt, contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        showPhotoSourceDialog = false
                                        when (PackageManager.PERMISSION_GRANTED) {
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                                cameraLauncher.launch(photoUri)
                                            }
                                            else -> {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showPhotoSourceDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showImagePreview && it.profileImgUrl != null) {
                    ImageDetailDialog(
                        images = listOf(it.profileImgUrl),
                        initialIndex = 0,
                        onDismiss = { showImagePreview = false },
                        onDelete = {},
                        showDelete = false,
                        showCounter = false,
                        onEdit = { showPhotoSourceDialog = true }
                    )
                }

                // Trigger gallery picker and handle crop like building
                if (showPicker) {
                    PhotoSelectorView(
                        maxSelectionCount = 1,
                        onImagesSelected = { uris ->
                            if (uris.isNotEmpty()) {
                                viewModel.updateProfileImage(uris.first())
                            }
                            showPicker = false
                        },
                        selectedImages = emptyList(),
                        aspectRatioX = 1f,
                        aspectRatioY = 1f,
                        maxResultWidth = 800,
                        maxResultHeight = 800,
                        launchKey = pickerLaunchKey,
                        useCircularFrame = true
                    )
                }

                // Handle cropping for camera image using the same UCrop flow
                LaunchedEffect(pendingCropUri) {
                    pendingCropUri?.let { uri ->
                        val destinationUri = Uri.fromFile(
                            File(context.cacheDir, "camera_cropped_${System.currentTimeMillis()}.jpg")
                        )
                        val options = UCrop.Options().apply {
                            setCircleDimmedLayer(true)
                            setShowCropFrame(false)
                            setShowCropGrid(false)
                            setHideBottomControls(false)
                            setCompressionQuality(90)
                            setCompressionFormat(Bitmap.CompressFormat.JPEG)
                        }
                        val uCropIntent = UCrop.of(uri, destinationUri)
                            .withAspectRatio(1f, 1f)
                            .withMaxResultSize(800, 800)
                            .withOptions(options)
                            .getIntent(context)
                        cropLauncher.launch(uCropIntent)
                    }
                }
            }
 ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Không thể tải thông tin người dùng.")
            }
        }
    }
}

@Composable
private fun HeaderSummary(viewModel: PersonalInformationVM) {
    val header by viewModel.headerState.collectAsState()
    when (val h = header) {
        is PersonalInformationVM.ProfileHeaderState.Landlord -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(title = "Buildings", value = h.buildings.toString(), modifier = Modifier.weight(1f))
                SummaryCard(title = "Rooms", value = h.rooms.toString(), modifier = Modifier.weight(1f))
                SummaryCard(title = "Tenants", value = h.tenants.toString(), modifier = Modifier.weight(1f))
            }
        }
        is PersonalInformationVM.ProfileHeaderState.Tenant -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(title = "Building", value = h.buildingName ?: "-", modifier = Modifier.weight(1f))
                SummaryCard(title = "Room", value = h.roomName ?: "-", modifier = Modifier.weight(1f))
            }
        }
        else -> Unit
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            // Avatar skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Header cards skeleton
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(3) {
                            ElevatedCard(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(30.dp)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Action buttons skeleton
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                    if (index < 3) Spacer(modifier = Modifier.height(10.dp))
                    else Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun PersonalInformationContent(
    user: User,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToSignOut: () -> Unit,
    onNavigateToEditPersonalInformation: () -> Unit,
    onAvatarClick: () -> Unit = {},
    onAvatarLongClick: () -> Unit = {},
    headerViewModel: PersonalInformationVM,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val sizePx = with(density) { 250.dp.toPx() }.toInt()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                    if (user.profileImgUrl != null) {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(user.profileImgUrl)
                                    .size(sizePx, sizePx)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .networkCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(300)
                                    .allowHardware(true)
                                    .build(),
                                contentDescription = "Upload Image success",
                                contentScale = ContentScale.Companion.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        onClick = onAvatarClick,
                                        onLongClick = onAvatarLongClick
                                    ),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Companion.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.Companion.size(40.dp),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Companion.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.BrokenImage,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(150.dp)
                                                    .clip(CircleShape),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.6f
                                                )
                                            )
                                            Text(
                                                "Image unavailable",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                            )

                            // Small edit button overlay (bottom-right)
                            Surface(
                                onClick = onAvatarLongClick,
                                shape = CircleShape,
                                tonalElevation = 4.dp,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                            ) {
                                IconButton(onClick = onAvatarLongClick) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .combinedClickable(
                                        onClick = onAvatarClick,
                                        onLongClick = onAvatarLongClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Add Photo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Small edit button overlay (bottom-right)
                            Surface(
                                onClick = onAvatarLongClick,
                                shape = CircleShape,
                                tonalElevation = 4.dp,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                            ) {
                                IconButton(onClick = onAvatarLongClick) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = user.fullName, style = MaterialTheme.typography.titleMedium)
                    // Header summary cards
                    Spacer(modifier = Modifier.height(16.dp))
                    HeaderSummary(viewModel = headerViewModel)
                }
            }
        }

        // Actions without the Account header; show Sign out prominently
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onNavigateToEditPersonalInformation, modifier = Modifier.fillMaxWidth()) {
                    Text("View info")
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onNavigateToChangePassword, modifier = Modifier.fillMaxWidth()) {
                    Text("Change password")
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = { /* TODO: show policies & terms */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Policies & Terms")
                }
                Spacer(modifier = Modifier.height(20.dp))
                var showSignOutConfirm by remember { mutableStateOf(false) }
                Button(
                    onClick = { showSignOutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Sign out") }
                if (showSignOutConfirm) {
                    AlertDialog(
                        onDismissRequest = { showSignOutConfirm = false },
                        title = { Text("Sign out") },
                        text = { Text("Are you sure you want to sign out?") },
                        confirmButton = {
                            Button(onClick = { showSignOutConfirm = false; onNavigateToSignOut() }) { Text("Yes") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}