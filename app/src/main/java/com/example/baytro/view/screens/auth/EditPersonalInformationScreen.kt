package com.example.baytro.view.screens.auth

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.baytro.auth.EditPersonalInformationFormState
import com.example.baytro.auth.RoleFormState
import com.example.baytro.data.user.Gender
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.IDCardImages
import com.example.baytro.view.components.RequiredDateTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.viewModel.auth.EditPersonalInformationVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonalInformationScreen (
    viewModel: EditPersonalInformationVM  = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPersonalInformation: () -> Unit,
) {
    val formState by viewModel.editPersonalInformationFormState.collectAsState()
    val roleFormState by viewModel.editRoleInformationFormState.collectAsState()
    val uiState by viewModel.editPersonalInformationUIState.collectAsState()

    LaunchedEffect(Unit) {
        Log.d("EditPersonalInformationScreen", "Screen launched - Checking if need to load")
        viewModel.loadEditPersonalInformation()
    }

    if (uiState == AuthUIState.Loading) {
        // Chưa load xong => hiển thị màn loading
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // Load xong => hiển thị UI chính
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Personal Information") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            EditPersonalInformationContent(
                formState = formState,
                roleFormState = roleFormState,
                uiState = uiState,
                onFullNameChange = viewModel::onFullNameChange,
                onDateOfBirthChange = viewModel::onDateOfBirthChange,
                onAddressChange = viewModel::onAddressChange,
                onPhoneNumberChange = viewModel::onPhoneNumberChange,
                onGenderChange = viewModel::onGenderChange,
                onBankCodeChange = viewModel::onBankCodeChange,
                onBankAccountNumberChange = viewModel::onBankAccountNumberChange,
                onChangePersonalInformationClicked = viewModel:: onChangePersonalInformationClicked,
                onNavigateToPersonalInformation = onNavigateToPersonalInformation,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun EditPersonalInformationContent(
    formState: EditPersonalInformationFormState,
    roleFormState: RoleFormState?,
    uiState: AuthUIState,
    onFullNameChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onBankCodeChange: (String) -> Unit,
    onBankAccountNumberChange: (String) -> Unit,
    onChangePersonalInformationClicked: () -> Unit,
    onNavigateToPersonalInformation: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            RequiredTextField (
                value = formState.fullName,
                onValueChange = onFullNameChange,
                label = "Full name",
                isError = false,
                errorMessage = null,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
        }

        item {
            RequiredDateTextField (
                selectedDate = formState.dateOfBirth,
                onDateSelected = onDateOfBirthChange,
                label = "Date of birth",
                isError = !formState.dateOfBirthError.isSuccess,
                errorMessage = formState.dateOfBirthError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
        }

        item {
            RequiredTextField (
                value = formState.address,
                onValueChange = onAddressChange,
                label = "Address",
                isError = false,
                errorMessage = null,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )

            Spacer (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
        }

        item {
            ChoiceSelection(
                options = Gender.entries.toList().dropLast(1),
                selectedOption = formState.gender,
                onOptionSelected = onGenderChange,
                isError = false,
                errorMessage = null
            )
        }

        item {
            RequiredTextField (
                value = formState.phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = "Phone number",
                isError = !formState.phoneNumberError.isSuccess,
                errorMessage = formState.phoneNumberError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
        }

        item {
            RequiredTextField (
                value = formState.email,
                onValueChange = {},
                label = "Email",
                isError = false,
                errorMessage = null,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
        }

        when (roleFormState) {
            is RoleFormState.Tenant -> {
                item {
                    DividerWithSubhead(
                        subhead = "ID card information"
                    )

                    IDCardImages(
                        idCardFrontImageUrl = roleFormState.idCardImageFrontUrl,
                        idCardBackImageUrl = roleFormState.idCardImageBackUrl
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )
                }

                item {
                    RequiredTextField(
                        value = roleFormState.idCardNumber,
                        onValueChange = {},
                        label = "Id card number",
                        isError = false,
                        errorMessage = null,
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )
                }

                item {
                    RequiredTextField(
                        value = roleFormState.idCardIssueDate,
                        onValueChange = {},
                        label = "Issue date",
                        isError = false,
                        errorMessage = null,
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )
                }
            }

            is RoleFormState.Landlord -> {
                item {
                    DividerWithSubhead(
                        subhead = "Bank information"
                    )

                    RequiredTextField(
                        value = roleFormState.bankCode,
                        onValueChange = onBankCodeChange,
                        label = "Bank",
                        isError = false,
                        errorMessage = null,
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )
                }

                item {
                    RequiredTextField(
                        value = roleFormState.bankAccountNumber,
                        onValueChange = onBankAccountNumberChange,
                        label = "Bank Account Number",
                        isError = false,
                        errorMessage = null,
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )
                }
            }

            else -> {
                item {
                    Text("fail to get role information")
                }
            }
        }

        item {
            SubmitButton(
                text = "Submit",
                isLoading = uiState is AuthUIState.Loading,
                onClick = {
                    onChangePersonalInformationClicked
                    onNavigateToPersonalInformation
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }
    }
}