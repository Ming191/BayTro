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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun PhotoCarousel(
    selectedPhotos: List<Uri>,
    onPhotosSelected: (List<Uri>) -> Unit,
    maxSelectionCount: Int = 5,
) {
    var showPicker by remember { mutableStateOf(false) }
    var selectedPhotoForDetail by remember { mutableStateOf<Uri?>(null) }

    if (showPicker) {
        PhotoSelectorView(
            maxSelectionCount = maxSelectionCount - selectedPhotos.size,
            onImagesSelected = { newPhotos ->
                onPhotosSelected(selectedPhotos + newPhotos)
                showPicker = false
            },
            selectedImages = emptyList()
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

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(selectedPhotos) { uri ->
            Box(
                modifier = Modifier.size(width = 150.dp, height = 200.dp)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 150.dp, height = 200.dp)
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
                        .size(width = 150.dp, height = 200.dp)
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
