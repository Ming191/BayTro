package com.example.baytro.view.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yalantis.ucrop.UCrop
import java.io.File

@Composable
fun PhotoSelectorView(
    maxSelectionCount: Int = 1,
    onImagesSelected: (List<Uri>) -> Unit = {},
    selectedImages: List<Uri> = emptyList(),
    aspectRatioX: Float = 3f,
    aspectRatioY: Float = 4f,
    maxResultWidth: Int = 1080,
    maxResultHeight: Int = 1440,
    launchKey: Int = 0
) {
    val context = LocalContext.current
    var pendingCropUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var currentCropIndex by remember { mutableIntStateOf(0) }
    val croppedUris = remember { mutableListOf<Uri>() }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { intent ->
                val croppedUri = UCrop.getOutput(intent)
                croppedUri?.let { croppedUris.add(it) }
            }
        }

        if (currentCropIndex + 1 < pendingCropUris.size) {
            currentCropIndex++
        } else {
            onImagesSelected(croppedUris.toList())
            croppedUris.clear()
            pendingCropUris = emptyList()
            currentCropIndex = 0
        }
    }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            pendingCropUris = listOf(it)
            currentCropIndex = 0
        }
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxSelectionCount.coerceAtLeast(2))
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingCropUris = uris
            currentCropIndex = 0
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(pendingCropUris, currentCropIndex) {
        if (pendingCropUris.isNotEmpty() && currentCropIndex < pendingCropUris.size) {
            val destinationUri = Uri.fromFile(
                File(
                    context.cacheDir,
                    "cropped_${System.currentTimeMillis()}_$currentCropIndex.jpg"
                )
            )
            val options = UCrop.Options().apply {
                // Toolbar colors
                setToolbarColor(primaryColor)
                setToolbarWidgetColor(android.graphics.Color.WHITE)
                setStatusBarColor(primaryColor)

                // UI colors
                setActiveControlsWidgetColor(primaryColor)
                setRootViewBackgroundColor(surfaceColor)
                setLogoColor(primaryColor)

                // Other customizations
                setToolbarTitle("Crop Image")
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false)
                setShowCropGrid(true)
                setCompressionQuality(90)
            }

            val uCropIntent = UCrop.of(pendingCropUris[currentCropIndex], destinationUri)
                .withAspectRatio(aspectRatioX, aspectRatioY)
                .withMaxResultSize(maxResultWidth, maxResultHeight)
                .withOptions(options)
                .getIntent(context)
            cropLauncher.launch(uCropIntent)
        }
    }

    fun launchPhotoPicker() {
        if (maxSelectionCount > 1) {
            multiplePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            singlePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    LaunchedEffect(launchKey) {
        if (launchKey > 0) {
            launchPhotoPicker()
        }
    }

    ImageLayoutView(selectedImages = selectedImages)
}


@Composable
fun ImageLayoutView(selectedImages: List<Uri>) {
    if (selectedImages.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(selectedImages) { uri ->
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
