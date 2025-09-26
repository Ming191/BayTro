package com.example.baytro.view.screens.splash

import RequiredDateTextField
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.example.baytro.data.BankCode
import com.example.baytro.data.Gender
import com.example.baytro.data.User
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.splash.NewLandlordUserFormState
import com.example.baytro.viewModel.splash.NewLandlordUserVM
import com.yalantis.ucrop.UCrop
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

@Composable
fun NewLandlordUserScreen(
    viewModel: NewLandlordUserVM = koinViewModel(),
    onComplete: () -> Unit
) {
    val formState by viewModel.newLandlordUserFormState.collectAsState()
    val newLandlordUserUIState by viewModel.newLandlordUserUIState.collectAsState()

    val context = LocalContext.current
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { intent ->
                val croppedUri = UCrop.getOutput(intent)
                croppedUri?.let { viewModel.onAvatarUriChange(it) }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val destinationUri = Uri.fromFile(
                File(context.cacheDir, "avatar_cropped_${System.currentTimeMillis()}.jpg")
            )
            val uCropIntent = UCrop.of(it, destinationUri)
                .withAspectRatio(1f, 1f) // crop vuông
                .withMaxResultSize(512, 512)
                .getIntent(context)
            cropLauncher.launch(uCropIntent)
        }
    }

    NewLandlordUserScreenContent(
        formState = formState,
        avatarUri = formState.avatarUri,
        onAvatarClick = { photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        ) },
        onFullNameChange = viewModel::onFullNameChange,
        onAddressChange = viewModel::onPermanentAddressChange,
        onGenderChange = viewModel::onGenderChange,
        onDateOfBirthChange = viewModel::onDateOfBirthChange,
        onBankCodeChange = viewModel::onBankCodeChange,
        onBankAccountNumberChange = viewModel::onBankAccountNumberChange,
        onSubmitClick = viewModel::submit,
        onPhoneNumberChange = viewModel::onPhoneNumberChange,
        newLandlordUserUIState = newLandlordUserUIState

    )

    LaunchedEffect(newLandlordUserUIState) {
        when (newLandlordUserUIState) {
            is UiState.Success -> {
                onComplete()
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLandlordUserScreenContent(
    formState: NewLandlordUserFormState,
    avatarUri: Uri?,
    onAvatarClick: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onBankCodeChange: (BankCode) -> Unit,
    onBankAccountNumberChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    newLandlordUserUIState: UiState<User>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    "Welcome, landlord!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                ) }
            )
        },
        bottomBar =  {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(
                        bottom = 32.dp,
                        start = 16.dp,
                        end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                SubmitButton(
                    isLoading = newLandlordUserUIState is UiState.Loading,
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
            // Avatar
            item {
                DividerWithSubhead("Profile image")
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .clickable { onAvatarClick() }
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != Uri.EMPTY) {
                        Image(
                            painter = rememberAsyncImagePainter(avatarUri),
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Placeholder",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Personal info
            item { DividerWithSubhead(
                "Personal information",
                modifier = Modifier.padding(vertical = 8.dp)
            ) }

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
                    errorMessage =  formState.dateOfBirthError.let {
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
                    modifier = Modifier
                        .fillMaxWidth(),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DropdownSelectField(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        label = "Gender",
                        options = Gender.entries.toList(),
                        selectedOption = formState.gender,
                        onOptionSelected = { onGenderChange(it) }
                    )
                    DropdownSelectField(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        label = "Bank code",
                        options = BankCode.entries.toList(),
                        selectedOption = formState.bankCode,
                        onOptionSelected = { onBankCodeChange(it) },
                        isLowerCased = false
                    )
                }
            }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = formState.bankAccountNumber,
                    onValueChange = onBankAccountNumberChange,
                    label = "Bank account number",
                    isError = formState.bankAccountNumberError is ValidationResult.Error,
                    errorMessage = formState.bankAccountNumberError.let {
                        if (it is ValidationResult.Error) it.message else null
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewNewLandlordUserScreen() {
    NewLandlordUserScreenContent(
        formState = NewLandlordUserFormState(),
        avatarUri = null,
        onAvatarClick = {},
        onFullNameChange = {},
        onAddressChange = {},
        onGenderChange = {},
        onDateOfBirthChange = {},
        onBankCodeChange = {},
        onBankAccountNumberChange = {},
        onPhoneNumberChange = {},
        onSubmitClick = {},
        newLandlordUserUIState = UiState.Idle
    )
}

