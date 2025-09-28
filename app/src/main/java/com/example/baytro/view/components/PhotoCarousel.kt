package com.example.baytro.view.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

enum class CarouselOrientation {
    Horizontal, Vertical
}

@Composable
fun PhotoCarousel(
    selectedPhotos: List<Uri>,
    onPhotosSelected: (List<Uri>) -> Unit,
    maxSelectionCount: Int = 5,
    orientation: CarouselOrientation = CarouselOrientation.Horizontal,
    imageWidth: Dp = 150.dp,
    imageHeight: Dp = 200.dp,
    aspectRatioX: Float = 3f,
    aspectRatioY: Float = 4f,
    maxResultWidth: Int = 1080,
    maxResultHeight: Int = 1440
) {
    var showPicker by remember { mutableStateOf(false) }
    var selectedPhotoForDetail by remember { mutableStateOf<Uri?>(null) }

    if (showPicker) {
        PhotoSelectorView(
            maxSelectionCount = maxSelectionCount - selectedPhotos.size,
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
            maxResultHeight = maxResultHeight
        )
    }

    selectedPhotoForDetail?.let { photoUri ->
        PhotoDetailDialog(
            photoUri = photoUri,
            onDismiss = { selectedPhotoForDetail = null },
            onDelete = {
                onPhotosSelected(selectedPhotos.filter { it != photoUri })
                selectedPhotoForDetail = null
            }
        )
    }

    val itemModifier = Modifier.size(imageWidth, imageHeight)
    val arrangement = Arrangement.spacedBy(8.dp)

    if (orientation == CarouselOrientation.Horizontal) {
        LazyRow(
            horizontalArrangement = arrangement,
        ) {
            items(selectedPhotos) { uri ->
                Box(
                    modifier = itemModifier
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(imageWidth, imageHeight)
                            .padding(4.dp)
                            .clickable { selectedPhotoForDetail = uri }
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Delete button
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
                            onClick = { onPhotosSelected(selectedPhotos.filter { it != uri }) },
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

            if (selectedPhotos.size < maxSelectionCount) {
                item {
                    Surface(
                        modifier = Modifier
                            .size(imageWidth, imageHeight)
                            .clickable { showPicker = true },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        shape = RoundedCornerShape(8.dp)
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
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = arrangement,
        ) {
            items(selectedPhotos) { uri ->
                Box(
                    modifier = itemModifier
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(imageWidth, imageHeight)
                            .padding(4.dp)
                            .clickable { selectedPhotoForDetail = uri }
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Delete button
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
                            onClick = { onPhotosSelected(selectedPhotos.filter { it != uri }) },
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

            if (selectedPhotos.size < maxSelectionCount) {
                item {
                    Surface(
                        modifier = Modifier
                            .size(imageWidth, imageHeight)
                            .clickable { showPicker = true },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        shape = RoundedCornerShape(8.dp)
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
            }
        }
    }
}
