package com.example.baytro.view.components

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.SubcomposeAsyncImage
import com.yalantis.ucrop.UCrop
import java.io.File

enum class CarouselOrientation {
    Horizontal, Vertical
}

@Composable
private fun ExistingImageItem(
    imageUrl: String,
    imageWidth: Dp,
    imageHeight: Dp,
    imageShape: Shape,
    onImageClick: () -> Unit,
    onDelete: () -> Unit,
    showDeleteButton: Boolean
) {
    Box(
        modifier = Modifier.size(imageWidth, imageHeight)
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(imageWidth, imageHeight)
                .padding(4.dp)
                .clickable { onImageClick() }
                .clip(imageShape),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )
            }
        )

        if (showDeleteButton) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .offset(x = (-2).dp, y = 2.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                tonalElevation = 4.dp
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete photo",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NewImageItem(
    uri: Uri,
    imageWidth: Dp,
    imageHeight: Dp,
    imageShape: Shape,
    onImageClick: () -> Unit,
    onDelete: () -> Unit,
    showDeleteButton: Boolean
) {
    Box(
        modifier = Modifier.size(imageWidth, imageHeight)
    ) {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .size(imageWidth, imageHeight)
                .padding(4.dp)
                .clickable { onImageClick() }
                .clip(imageShape),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )
            }
        )

        if (showDeleteButton) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .offset(x = (-2).dp, y = 2.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                tonalElevation = 4.dp
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete photo",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadButton(
    imageWidth: Dp,
    imageHeight: Dp,
    imageShape: Shape,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(imageWidth, imageHeight)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shape = imageShape
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = "Add Photo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Upload",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photo") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // From Gallery option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onGallerySelected()
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "From Gallery",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "From Gallery",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Take Photo option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onCameraSelected()
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Take Photo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Take Photo",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PhotoCarousel(
    selectedPhotos: List<Uri>,
    onPhotosSelected: (List<Uri>) -> Unit,
    existingImageUrls: List<String> = emptyList(),
    onExistingImagesChanged: (List<String>) -> Unit = {},
    maxSelectionCount: Int = 5,
    orientation: CarouselOrientation = CarouselOrientation.Horizontal,
    imageWidth: Dp = 150.dp,
    imageHeight: Dp = 200.dp,
    aspectRatioX: Float = 3f,
    aspectRatioY: Float = 4f,
    maxResultWidth: Int = 1080,
    maxResultHeight: Int = 1440,
    useCircularFrame: Boolean = false,
    showDeleteButton: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showImageDetailDialog by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(-1) }
    var pickerLaunchKey by remember { mutableIntStateOf(0) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val totalImageCount = existingImageUrls.size + selectedPhotos.size

    val allImageModels = remember(existingImageUrls, selectedPhotos) {
        existingImageUrls + selectedPhotos.map { it.toString() }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                pendingCropUri = uri
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val imagesDir = File(context.cacheDir, "images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val imageFile = File(imagesDir, "camera_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera not available: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { intent ->
                val croppedUri = UCrop.getOutput(intent)
                croppedUri?.let { uri ->
                    onPhotosSelected(selectedPhotos + uri)
                }
            }
        }
        pendingCropUri = null
    }

    LaunchedEffect(pendingCropUri) {
        pendingCropUri?.let { uri ->
            val imagesDir = File(context.cacheDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val destinationUri = Uri.fromFile(
                File(imagesDir, "camera_cropped_${System.currentTimeMillis()}.jpg")
            )
            val uCropIntent = UCrop.of(uri, destinationUri)
                .withAspectRatio(aspectRatioX, aspectRatioY)
                .withMaxResultSize(maxResultWidth, maxResultHeight)
                .getIntent(context)
            cropLauncher.launch(uCropIntent)
        }
    }

    if (showPicker) {
        PhotoSelectorView(
            maxSelectionCount = maxSelectionCount - totalImageCount,
            onImagesSelected = { newPhotos ->
                if (newPhotos.isNotEmpty()) {
                    onPhotosSelected(selectedPhotos + newPhotos)
                }
                showPicker = false
            },
            selectedImages = emptyList(),
            aspectRatioX = aspectRatioX,
            aspectRatioY = aspectRatioY,
            maxResultWidth = maxResultWidth,
            maxResultHeight = maxResultHeight,
            launchKey = pickerLaunchKey
        )
    }

    if (showImageDetailDialog && selectedImageIndex != -1 && allImageModels.isNotEmpty()) {
        // Use a key to force recreation when the images list changes
        key(allImageModels.joinToString(",")) {
            ImageDetailDialog(
                images = allImageModels,
                initialIndex = selectedImageIndex.coerceIn(0, allImageModels.size - 1),
                onDismiss = {
                    showImageDetailDialog = false
                    selectedImageIndex = -1
                },
                onDelete = { index ->
                    if (index < existingImageUrls.size) {
                        val imageUrl = existingImageUrls[index]
                        onExistingImagesChanged(existingImageUrls.filter { it != imageUrl })
                    } else {
                        val uriIndex = index - existingImageUrls.size
                        if (uriIndex < selectedPhotos.size) {
                            val uri = selectedPhotos[uriIndex]
                            onPhotosSelected(selectedPhotos.filter { it != uri })
                        }
                    }
                    val totalImages = existingImageUrls.size + selectedPhotos.size - 1
                    if (totalImages <= 0) {
                        showImageDetailDialog = false
                        selectedImageIndex = -1
                    } else if (selectedImageIndex >= totalImages) {
                        selectedImageIndex = totalImages - 1
                    }
                },
                showDelete = showDeleteButton
            )
        }
    }

    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSourceDialog = false },
            onGallerySelected = {
                pickerLaunchKey++
                showPicker = true
            },
            onCameraSelected = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    val imageShape = if (useCircularFrame) CircleShape else RoundedCornerShape(8.dp)
    val arrangement = Arrangement.spacedBy(8.dp)

    Column {
        if (orientation == CarouselOrientation.Horizontal) {
            LazyRow(
                horizontalArrangement = arrangement,
            ) {
                // Existing images from URLs
                itemsIndexed(
                    items = existingImageUrls,
                    key = { _, imageUrl -> "existing_$imageUrl" }
                ) { index, imageUrl ->
                    ExistingImageItem(
                        imageUrl = imageUrl,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        imageShape = imageShape,
                        onImageClick = {
                            selectedImageIndex = index
                            showImageDetailDialog = true
                        },
                        onDelete = {
                            onExistingImagesChanged(existingImageUrls.filter { it != imageUrl })
                        },
                        showDeleteButton = showDeleteButton
                    )
                }

                itemsIndexed(
                    items = selectedPhotos,
                    key = { _, uri -> "new_$uri" }
                ) { uriIndex, uri ->
                    val index = existingImageUrls.size + uriIndex
                    NewImageItem(
                        uri = uri,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        imageShape = imageShape,
                        onImageClick = {
                            selectedImageIndex = index
                            showImageDetailDialog = true
                        },
                        onDelete = {
                            onPhotosSelected(selectedPhotos.filter { it != uri })
                        },
                        showDeleteButton = showDeleteButton
                    )
                }

                if (totalImageCount < maxSelectionCount) {
                    item {
                        UploadButton(
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            imageShape = imageShape,
                            onClick = { showPhotoSourceDialog = true }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = arrangement,
            ) {
                itemsIndexed(
                    items = existingImageUrls,
                    key = { _, imageUrl -> "existing_$imageUrl" }
                ) { index, imageUrl ->
                    ExistingImageItem(
                        imageUrl = imageUrl,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        imageShape = imageShape,
                        onImageClick = {
                            selectedImageIndex = index
                            showImageDetailDialog = true
                        },
                        onDelete = {
                            onExistingImagesChanged(existingImageUrls.filter { it != imageUrl })
                        },
                        showDeleteButton = showDeleteButton
                    )
                }

                itemsIndexed(
                    items = selectedPhotos,
                    key = { _, uri -> "new_$uri" }
                ) { uriIndex, uri ->
                    val index = existingImageUrls.size + uriIndex
                    NewImageItem(
                        uri = uri,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        imageShape = imageShape,
                        onImageClick = {
                            selectedImageIndex = index
                            showImageDetailDialog = true
                        },
                        onDelete = {
                            onPhotosSelected(selectedPhotos.filter { it != uri })
                        },
                        showDeleteButton = showDeleteButton
                    )
                }

                if (totalImageCount < maxSelectionCount) {
                    item {
                        UploadButton(
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            imageShape = imageShape,
                            onClick = { showPhotoSourceDialog = true }
                        )
                    }
                }
            }
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}