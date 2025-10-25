package com.example.baytro.view.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.yalantis.ucrop.UCrop
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.example.baytro.data.user.User
import com.example.baytro.data.user.Role
import com.example.baytro.viewModel.auth.PersonalInformationVM
import org.koin.compose.viewmodel.koinViewModel
import android.graphics.Bitmap
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.runtime.mutableIntStateOf
import com.example.baytro.view.components.PhotoSelectorView
import com.example.baytro.view.components.ImageDetailDialog
import com.example.baytro.utils.LocalAvatarCache

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PersonalInformationVM = koinViewModel(),
    onNavigateToChangePassword: () -> Unit,
    onNavigateToSignOut: () -> Unit,
    onNavigateToEditPersonalInformation: () -> Unit,
    onNavigateToPoliciesAndTerms: () -> Unit,
    onNavigateToPaymentSettings: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val avatarCache = LocalAvatarCache.current

    LaunchedEffect(Unit) {
        viewModel.loadPersonalInformation()
    }

    LaunchedEffect(user?.profileImgUrl) {
        user?.profileImgUrl?.let { avatarCache.updateAvatar(it) }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold {
            user?.let {
                var showPhotoSourceDialog by remember { mutableStateOf(false) }
                var showImagePreview by remember { mutableStateOf(false) }
                var showPicker by remember { mutableStateOf(false) }
                var pickerLaunchKey by remember { mutableIntStateOf(0) }
                var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

                val context = LocalContext.current

                val photoFile = remember {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    File(context.cacheDir, "profile_photo_$timeStamp.jpg")
                }

                val photoUri = remember {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                }

                val cropLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        UCrop.getOutput(result.data!!)?.let { croppedUri ->
                            viewModel.updateProfileImage(croppedUri)
                        }
                    }
                    pendingCropUri = null
                }

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success) pendingCropUri = photoUri
                }

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        cameraLauncher.launch(photoUri)
                    } else {
                        Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
                    }
                }

                ProfileContent(
                    user = it,
                    viewModel = viewModel,
                    onNavigateToChangePassword = onNavigateToChangePassword,
                    onNavigateToSignOut = onNavigateToSignOut,
                    onNavigateToEditPersonalInformation = onNavigateToEditPersonalInformation,
                    onNavigateToPoliciesAndTerms = onNavigateToPoliciesAndTerms,
                    onNavigateToPaymentSettings = onNavigateToPaymentSettings,
                    onAvatarClick = { showImagePreview = true },
                    onEditPhoto = { showPhotoSourceDialog = true },
                )

                if (showPhotoSourceDialog) {
                    ModalBottomSheet(
                        onDismissRequest = { showPhotoSourceDialog = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = "Add Photo",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )

                            PhotoOption(
                                icon = Icons.Outlined.Image,
                                title = "From Gallery",
                                onClick = {
                                    showPhotoSourceDialog = false
                                    pickerLaunchKey++
                                    showPicker = true
                                }
                            )

                            PhotoOption(
                                icon = Icons.Outlined.CameraAlt,
                                title = "Take Photo",
                                onClick = {
                                    showPhotoSourceDialog = false
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                            cameraLauncher.launch(photoUri)
                                        }
                                        else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            )
                        }
                    }
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
        }
    }
}

@Composable
private fun PhotoOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun HeaderStats(viewModel: PersonalInformationVM) {
    val header by viewModel.headerState.collectAsState()

    when (val h = header) {
        is PersonalInformationVM.ProfileHeaderState.Landlord -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Buildings", h.buildings.toString(), Modifier.weight(1f))
                StatCard("Rooms", h.rooms.toString(), Modifier.weight(1f))
                StatCard("Tenants", h.tenants.toString(), Modifier.weight(1f))
            }
        }
        is PersonalInformationVM.ProfileHeaderState.Tenant -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Building", h.buildingName ?: "-", Modifier.weight(1f))
                StatCard("Room", h.roomName ?: "-", Modifier.weight(1f))
            }
        }
        else -> Unit
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    viewModel: PersonalInformationVM,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToSignOut: () -> Unit,
    onNavigateToEditPersonalInformation: () -> Unit,
    onNavigateToPoliciesAndTerms: () -> Unit,
    onNavigateToPaymentSettings: () -> Unit,
    onAvatarClick: () -> Unit,
    onEditPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { 200.dp.toPx() }.toInt()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profileImgUrl != null) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(user.profileImgUrl)
                                .size(sizePx, sizePx)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .crossfade(300)
                                .allowHardware(true)
                                .build(),
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable(onClick = onAvatarClick),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClick = onEditPhoto),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Edit button
                    FilledIconButton(
                        onClick = onEditPhoto,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                HeaderStats(viewModel = viewModel)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    icon = Icons.Outlined.Person,
                    title = "Personal Information",
                    onClick = onNavigateToEditPersonalInformation
                )

                ActionButton(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    onClick = onNavigateToChangePassword
                )

                ActionButton(
                    icon = Icons.Outlined.Description,
                    title = "Policies & Terms",
                    onClick = onNavigateToPoliciesAndTerms
                )

                if (user.role is Role.Landlord) {
                    ActionButton(
                        icon = Icons.Outlined.Settings,
                        title = "Payment Settings",
                        onClick = onNavigateToPaymentSettings
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                var showSignOutDialog by remember { mutableStateOf(false) }

                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }

                if (showSignOutDialog) {
                    AlertDialog(
                        onDismissRequest = { showSignOutDialog = false },
                        title = { Text("Sign Out") },
                        text = { Text("Are you sure you want to sign out?") },
                        confirmButton = {
                            Button(onClick = {
                                showSignOutDialog = false
                                onNavigateToSignOut()
                            }) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSignOutDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}