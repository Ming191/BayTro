package com.example.baytro.view.screens.request

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.LoadingOverlay
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredDateTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.request.AddRequestVM
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRequestScreen(
    viewModel: AddRequestVM = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.addRequestUIState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            // SỬA 2: Kiểm tra đúng kiểu UiState.Success
            is UiState.Success -> {
                Toast.makeText(context, "Request submitted successfully!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onNavigateBack()
            }
            is UiState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        SubmitButton(
                            text = "Submit Request",
                            isLoading = uiState is UiState.Loading,
                            onClick = {
                                viewModel.addRequest(
                                    context = context,
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                item {
                    DividerWithSubhead(subhead = "Request Details")
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    RequiredTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        value = formState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = "Request Title",
                        isError = formState.titleError is ValidationResult.Error,
                        errorMessage = (formState.titleError as? ValidationResult.Error)?.message
                    )
                }

                item {
                    RequiredTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        value = formState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = "Description",
                        isError = formState.descriptionError is ValidationResult.Error,
                        errorMessage = (formState.descriptionError as? ValidationResult.Error)?.message,
                    )
                }

                item {
                    RequiredDateTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        label = "Scheduled Date",
                        selectedDate = formState.scheduledDate,
                        onDateSelected = { viewModel.updateScheduledDate(it) },
                        isError = formState.scheduledDateError is ValidationResult.Error,
                        errorMessage = (formState.scheduledDateError as? ValidationResult.Error)?.message
                    )
                }

                item {
                    DividerWithSubhead(subhead = "Attach Photos")
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    PhotoCarousel(
                        selectedPhotos = formState.selectedPhotos,
                        onPhotosSelected = { photos -> viewModel.updateSelectedPhotos(photos) }
                    )
                }
            }
        }

        if (uiState is UiState.Loading) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                LoadingOverlay(progress = uploadProgress)
            }
        }
    }
}