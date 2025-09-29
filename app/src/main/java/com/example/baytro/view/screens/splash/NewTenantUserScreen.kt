package com.example.baytro.view.screens.splash

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.Gender
import com.example.baytro.data.User
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredDateTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.splash.IdCardDataViewModel
import com.example.baytro.viewModel.splash.NewTenantUserFormState
import com.example.baytro.viewModel.splash.NewTenantUserVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewTenantUserScreen(
    viewModel: NewTenantUserVM = koinViewModel(),
    idCardDataViewModel: IdCardDataViewModel = koinInject(),
    onComplete: () -> Unit
) {
    val formState by viewModel.newTenantUserFormState.collectAsState()
    val newTenantUserUIState by viewModel.newTenantUserUIState.collectAsState()
    val idCardInfoWithImages by idCardDataViewModel.idCardInfoWithImages.collectAsState()

    Log.d("NewTenantUserScreen", "Composing screen with shared idCardInfoWithImages: $idCardInfoWithImages")

    LaunchedEffect(idCardInfoWithImages) {
        Log.d("NewTenantUserScreen", "LaunchedEffect triggered with idCardInfoWithImages: $idCardInfoWithImages")
        idCardInfoWithImages?.let { infoWithImages ->
            Log.i("NewTenantUserScreen", "Calling prefillWithIdCardInfo with: ${infoWithImages.idCardInfo}")
            Log.i("NewTenantUserScreen", "ID card images - Front: ${infoWithImages.frontImageUrl}, Back: ${infoWithImages.backImageUrl}")
            viewModel.prefillWithIdCardInfo(infoWithImages.idCardInfo, infoWithImages.frontImageUrl, infoWithImages.backImageUrl)
            idCardDataViewModel.clearIdCardInfo()
        } ?: Log.w("NewTenantUserScreen", "No idCardInfoWithImages in shared state, skipping prefill")
    }

    LaunchedEffect(formState) {
        Log.d("NewTenantUserScreen", "Form state updated: name='${formState.fullName}', id='${formState.idCardNumber}', dob='${formState.dateOfBirth}'")
    }

    NewTenantUserScreenContent(
        formState = formState,
        selectedPhotos = if (formState.avatarUri != Uri.EMPTY) listOf(formState.avatarUri) else emptyList(),
        onPhotosSelected = { photos ->
            viewModel.onAvatarUriChange(photos.firstOrNull() ?: Uri.EMPTY)
        },
        onFullNameChange = viewModel::onFullNameChange,
        onAddressChange = viewModel::onPermanentAddressChange,
        onGenderChange = viewModel::onGenderChange,
        onDateOfBirthChange = viewModel::onDateOfBirthChange,
        onPhoneNumberChange = viewModel::onPhoneNumberChange,
        onOccupationChange = viewModel::onOccupationChange,
        onIdCardNumberChange = viewModel::onIdCardNumberChange,
        onIdCardIssueDateChange = viewModel::onIdCardIssueDateChange,
        onEmergencyContactChange = viewModel::onEmergencyContactChange,
        onSubmitClick = viewModel::submit,
        newTenantUserUIState = newTenantUserUIState
    )

    LaunchedEffect(newTenantUserUIState) {
        when (newTenantUserUIState) {
            is UiState.Success -> {
                onComplete()
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTenantUserScreenContent(
    formState: NewTenantUserFormState,
    selectedPhotos: List<Uri>,
    onPhotosSelected: (List<Uri>) -> Unit,
    onFullNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onOccupationChange: (String) -> Unit,
    onIdCardNumberChange: (String) -> Unit,
    onIdCardIssueDateChange: (String) -> Unit,
    onEmergencyContactChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    newTenantUserUIState: UiState<User>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    "Welcome, tenant!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                ) }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(
                        bottom = 32.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                SubmitButton(
                    isLoading = newTenantUserUIState is UiState.Loading,
                    onClick = onSubmitClick,
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                DividerWithSubhead("Profile image")
                PhotoCarousel(
                    selectedPhotos = selectedPhotos,
                    onPhotosSelected = onPhotosSelected,
                    maxSelectionCount = 1,
                    imageWidth = 150.dp,
                    imageHeight = 150.dp,
                    aspectRatioX = 1f,
                    aspectRatioY = 1f,
                    maxResultWidth = 512,
                    maxResultHeight = 512,
                    useCircularFrame = true
                )
                AnimatedVisibility(visible = formState.avatarUriError is ValidationResult.Error) {
                    (formState.avatarUriError as? ValidationResult.Error)?.let { error ->
                        Text(
                            text = error.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Personal info
            item {
                DividerWithSubhead(
                    "Personal information",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.fullName,
                    onValueChange = onFullNameChange,
                    label = "Full name",
                    isError = formState.fullNameError is ValidationResult.Error,
                    errorMessage = formState.fullNameError.let {
                        if (it is ValidationResult.Error) it.message else null
                    }
                )
            }

            item {
                RequiredDateTextField(
                    label = "Date of Birth",
                    selectedDate = formState.dateOfBirth,
                    onDateSelected = onDateOfBirthChange,
                    isError = formState.dateOfBirthError is ValidationResult.Error,
                    errorMessage = formState.dateOfBirthError.let {
                        if (it is ValidationResult.Error) it.message else null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = "Phone number",
                    isError = formState.phoneNumberError is ValidationResult.Error,
                    errorMessage = formState.phoneNumberError.let {
                        if (it is ValidationResult.Error) it.message else null
                    }
                )
            }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.permanentAddress,
                    onValueChange = onAddressChange,
                    label = "Permanent address",
                    isError = formState.permanentAddressError is ValidationResult.Error,
                    errorMessage = formState.permanentAddressError.let {
                        if (it is ValidationResult.Error) it.message else null
                    }
                )
            }

            item {
                DropdownSelectField(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Gender",
                    options = Gender.entries.toList(),
                    selectedOption = formState.gender,
                    onOptionSelected = { onGenderChange(it) },
                    optionToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                )
            }

            item {
                DividerWithSubhead(
                    "Tenant information",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.occupation,
                    onValueChange = onOccupationChange,
                    label = "Occupation",
                    isError = formState.occupationError is ValidationResult.Error,
                    errorMessage = formState.occupationError.let {
                        if (it is ValidationResult.Error) it.message else null
                    }
                )
            }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.idCardNumber,
                    onValueChange = onIdCardNumberChange,
                    label = "ID Card Number",
                    isError = formState.idCardNumberError is ValidationResult.Error,
                    errorMessage = formState.idCardNumberError.let {
                        if (it is ValidationResult.Error) it.message else null
                    }
                )
            }

            item {
                RequiredDateTextField(
                    label = "ID Card Issue Date",
                    selectedDate = formState.idCardIssueDate,
                    onDateSelected = onIdCardIssueDateChange,
                    isError = formState.idCardIssueDateError is ValidationResult.Error,
                    errorMessage = formState.idCardIssueDateError.let {
                        if (it is ValidationResult.Error) it.message else null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.emergencyContact,
                    onValueChange = onEmergencyContactChange,
                    label = "Emergency Contact",
                    isError = formState.emergencyContactError is ValidationResult.Error,
                    errorMessage = formState.emergencyContactError.let {
                        if (it is ValidationResult.Error) it.message else null
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewNewTenantUserScreen() {
    NewTenantUserScreenContent(
        formState = NewTenantUserFormState(),
        selectedPhotos = emptyList(),
        onPhotosSelected = {},
        onFullNameChange = {},
        onAddressChange = {},
        onGenderChange = {},
        onDateOfBirthChange = {},
        onPhoneNumberChange = {},
        onOccupationChange = {},
        onIdCardNumberChange = {},
        onIdCardIssueDateChange = {},
        onEmergencyContactChange = {},
        onSubmitClick = {},
        newTenantUserUIState = UiState.Idle
    )
}
