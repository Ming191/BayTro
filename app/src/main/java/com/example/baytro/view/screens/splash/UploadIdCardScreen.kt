package com.example.baytro.view.screens.splash

import android.util.Log
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
import androidx.compose.ui.unit.dp
import com.example.baytro.utils.errorMessage
import com.example.baytro.utils.isError
import com.example.baytro.view.components.CarouselOrientation
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.splash.IdCardDataViewModel
import com.example.baytro.viewModel.splash.UploadIdCardVM
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun UploadIdCardScreen(
    viewModel: UploadIdCardVM = koinViewModel(),
    idCardDataViewModel: IdCardDataViewModel = koinInject(),
    onNavigateToTenantForm: () -> Unit
) {
    val uiState by viewModel.uploadIdCardUiState.collectAsState()
    val formState by viewModel.uploadIdCardFormState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Log.d("UploadIdCardScreen", "Composing screen with UI state: ${uiState::class.simpleName}")

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success -> {
                Log.i("UploadIdCardScreen", "OCR processing completed successfully, storing data and navigating: ${state.data.idCardInfo}")
                Log.i("UploadIdCardScreen", "ID card images - Front: ${state.data.frontImageUrl}, Back: ${state.data.backImageUrl}")
                idCardDataViewModel.setIdCardInfo(state.data)
                onNavigateToTenantForm()
                viewModel.clearError()
            }
            is UiState.Error -> {
                Log.w("UploadIdCardScreen", "OCR processing failed: ${state.message}")
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearError()
            }
            is UiState.Loading -> {
                Log.d("UploadIdCardScreen", "OCR processing in progress...")
            }
            else -> {
                Log.v("UploadIdCardScreen", "UI state: ${state::class.simpleName}")
            }
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

            if (formState.photosError.isError()) {
                Text(
                    text = formState.photosError.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PhotoCarousel(
                selectedPhotos = formState.selectedPhotos,
                onPhotosSelected = viewModel::onPhotosChange,
                maxSelectionCount = 2,
                orientation = CarouselOrientation.Vertical,
                imageWidth = 400.dp,
                imageHeight = 250.dp,
                aspectRatioX = 4f,
                aspectRatioY = 2.5f,
                maxResultWidth = 1440,
                maxResultHeight = 1080,
            )

            Spacer(modifier = Modifier.weight(1f))

            SubmitButton(
                text = "Submit ID Card",
                isLoading = uiState is UiState.Loading,
                onClick = viewModel::onSubmit
            )
        }
    }
}
