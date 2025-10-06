package com.example.baytro.view.screens.splash

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.example.baytro.data.user.BankCode
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.User
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.RequiredDateTextField
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
    var isAvatarPickerOpen by remember { mutableStateOf(false) }
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
            isAvatarPickerOpen = false
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
        isAvatarPickerOpen = false
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
        onAvatarClick = {
            if (!isAvatarPickerOpen) {
                isAvatarPickerOpen = true
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        },
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
    val fullNameFocus = remember { FocusRequester() }
    val phoneNumberFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val bankAccountFocus = remember { FocusRequester() }

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
                DividerWithSubhead(
                    subhead = "Profile image",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .clickable { onAvatarClick() }
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null && avatarUri != Uri.EMPTY) {
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
            item { DividerWithSubhead(
                modifier = Modifier.padding(vertical = 8.dp),
                subhead = "Personal information",
            ) }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth().focusRequester(fullNameFocus),
                    value = formState.fullName,
                    onValueChange = onFullNameChange,
                    label = "Full name",
                    isError = formState.fullNameError is ValidationResult.Error,
                    errorMessage = formState.fullNameError.let {
                        if (it is ValidationResult.Error) it.message else null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { phoneNumberFocus.requestFocus() }
                    )
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
                    modifier = Modifier.fillMaxWidth().focusRequester(phoneNumberFocus),
                    value = formState.phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = "Phone number",
                    isError = formState.phoneNumberError is ValidationResult.Error,
                    errorMessage = formState.phoneNumberError.let {
                        if (it is ValidationResult.Error) it.message else null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { addressFocus.requestFocus() }
                    )
                )
            }

            item {
                RequiredTextField(
                    modifier = Modifier
                        .fillMaxWidth().focusRequester(addressFocus),
                    value = formState.permanentAddress,
                    onValueChange = onAddressChange,
                    label = "Permanent address",
                    isError = formState.permanentAddressError is ValidationResult.Error,
                    errorMessage = formState.permanentAddressError.let {
                        if (it is ValidationResult.Error) it.message else null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { bankAccountFocus.requestFocus() }
                    )
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
                        onOptionSelected = { onGenderChange(it) },
                        optionToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                    )
                    DropdownSelectField(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        label = "Bank code",
                        options = BankCode.entries.toList(),
                        selectedOption = formState.bankCode,
                        onOptionSelected = { onBankCodeChange(it) },
                        optionToString = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }

                    )
                }
            }

            item {
                RequiredTextField(
                    modifier = Modifier.fillMaxWidth().focusRequester(bankAccountFocus),
                    value = formState.bankAccountNumber,
                    onValueChange = onBankAccountNumberChange,
                    label = "Bank account number",
                    isError = formState.bankAccountNumberError is ValidationResult.Error,
                    errorMessage = formState.bankAccountNumberError.let {
                        if (it is ValidationResult.Error) it.message else null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { bankAccountFocus.freeFocus() }
                    )
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

