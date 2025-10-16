package com.example.baytro.view.screens.request

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.baytro.utils.errorMessage
import com.example.baytro.utils.isError
import com.example.baytro.view.components.CarouselOrientation
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.LoadingOverlay
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.request.AssignRequestVM
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssigningScreen(
    requestId: String,
    viewModel: AssignRequestVM = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.assignUiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    var nameVisible by remember { mutableStateOf(false) }
    var phoneVisible by remember { mutableStateOf(false) }
    var photoVisible by remember { mutableStateOf(false) }
    var buttonsVisible by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadRequest(requestId)
        delay(50)
        nameVisible = true
        delay(80)
        phoneVisible = true
        delay(80)
        photoVisible = true
        delay(80)
        buttonsVisible = true
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success -> {
                isNavigating = true
                Toast.makeText(context, "Request assigned successfully!", Toast.LENGTH_SHORT).show()
                onNavigateBack()
                viewModel.resetState()
            }
            is UiState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
            else -> { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {},
            bottomBar = {
                BottomAppBar {
                    AnimatedVisibility(
                        visible = buttonsVisible,
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
                            initialOffsetY = { it }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.weight(1f),
                                enabled = uiState !is UiState.Loading
                            ) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.padding(8.dp))
                            Button(
                                onClick = { viewModel.assignRequest() },
                                modifier = Modifier.weight(1f),
                                enabled = uiState !is UiState.Loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedVisibility(
                    visible = nameVisible,
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
                    RequiredTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = formState.assigneeName,
                        onValueChange = viewModel::onAssigneeNameChange,
                        label = "Assignee",
                        isError = formState.assigneeNameError.isError(),
                        errorMessage = if (formState.assigneeNameError.isError()) {
                            formState.assigneeNameError.errorMessage ?: "Invalid name"
                        } else null,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = phoneVisible,
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
                    RequiredTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = formState.assigneePhoneNumber,
                        onValueChange = viewModel::onAssigneePhoneNumberChange,
                        label = "Assignee's phone number",
                        isError = formState.assigneePhoneNumberError.isError(),
                        errorMessage = if (formState.assigneePhoneNumberError.isError()) {
                            formState.assigneePhoneNumberError.errorMessage
                                ?: "Invalid phone number"
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = photoVisible,
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
                    Column {
                        DividerWithSubhead(subhead = "Photo of the Issue (Max: 4)")
                        Spacer(modifier = Modifier.height(8.dp))
                        PhotoCarousel(
                            selectedPhotos = formState.selectedPhotos,
                            onPhotosSelected = viewModel::onPhotosSelected,
                            existingImageUrls = formState.existingImageUrls,
                            onExistingImagesChanged = {},
                            maxSelectionCount = 4,
                            orientation = CarouselOrientation.Horizontal,
                            showDeleteButton = false
                        )
                    }
                }
            }
        }

        if (uiState is UiState.Loading || isNavigating) {
            LoadingOverlay(progress = uploadProgress)
        }
    }
}