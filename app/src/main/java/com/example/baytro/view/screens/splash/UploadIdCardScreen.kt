package com.example.baytro.view.screens.splash

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.utils.errorMessage
import com.example.baytro.utils.isError
import com.example.baytro.view.components.CarouselOrientation
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.splash.UploadIdCardVM
import org.koin.androidx.compose.koinViewModel

@Preview(showBackground = true)
@Composable
fun UploadIdCardScreenPreview() {
    UploadIdCardScreen()
}

@Composable
fun UploadIdCardScreen(
    viewModel: UploadIdCardVM = koinViewModel()
) {
    val uiState by viewModel.uploadIdCardUiState.collectAsState()
    val formState by viewModel.uploadIdCardFormState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar(state.data)
                viewModel.clearError()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            // Title
            Text(
                text = "Upload ID Card",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Please upload both front and back of your ID card",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Show validation error if any
            if (formState.photosError.isError()) {
                Text(
                    text = formState.photosError.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vertical Photo Carousel
            PhotoCarousel(
                selectedPhotos = formState.selectedPhotos,
                onPhotosSelected = viewModel::onPhotosChange,
                maxSelectionCount = 2,
                orientation = CarouselOrientation.Vertical,
                imageWidth = 400.dp,
                imageHeight = 250.dp,
                aspectRatioX = 4f, // Landscape for ID cards
                aspectRatioY = 3f,
                maxResultWidth = 1440, // Swapped for landscape
                maxResultHeight = 1080
            )

            Spacer(modifier = Modifier.weight(1f))

            // Submit Button
            SubmitButton(
                text = "Submit ID Card",
                isLoading = uiState is UiState.Loading,
                onClick = viewModel::onSubmit
            )
        }
    }
}
