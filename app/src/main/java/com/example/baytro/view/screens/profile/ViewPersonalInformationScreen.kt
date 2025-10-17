package com.example.baytro.view.screens.profile

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.example.baytro.auth.EditPersonalInformationFormState
import com.example.baytro.auth.RoleFormState
import com.example.baytro.data.user.Gender
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.IDCardImages
import com.example.baytro.view.components.PersonalInformationCard
import com.example.baytro.view.AuthUIState
import com.example.baytro.viewModel.auth.EditPersonalInformationVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ViewPersonalInformationScreen(
    viewModel: EditPersonalInformationVM = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit
) {
    val formState by viewModel.editPersonalInformationFormState.collectAsState()
    val roleFormState: RoleFormState? by viewModel.editRoleInformationFormState.collectAsState()
    val uiState by viewModel.editPersonalInformationUIState.collectAsState()
    val isLoading = uiState is AuthUIState.Loading

    LaunchedEffect(Unit) {
        Log.d("ViewPersonalInformationScreen", "Screen launched - Loading personal information")
        viewModel.loadEditPersonalInformation()
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        roleFormState?.let { nonNullRole ->
            ViewPersonalInformationContent(
                formState = formState,
                roleFormState = nonNullRole,
                onNavigateToEdit = onNavigateToEdit
            )
        }
    }
}

@Composable
fun ViewPersonalInformationContent(
    formState: EditPersonalInformationFormState,
    roleFormState: RoleFormState,
    onNavigateToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                DividerWithSubhead(subhead = "Basic Information")
                Spacer(modifier = Modifier.height(8.dp))

                PersonalInformationCard(
                    infoMap = mapOf(
                        "Full Name" to formState.fullName,
                        "Date of Birth" to formState.dateOfBirth,
                        "Gender" to when (formState.gender) {
                            Gender.MALE -> "Male"
                            Gender.FEMALE -> "Female"
                            Gender.OTHER -> "Other"
                            else -> "Unknown"
                        },
                        "Phone Number" to formState.phoneNumber,
                        "Email" to formState.email,
                        "Address" to formState.address
                    )
                )
            }

            when (roleFormState) {
                is RoleFormState.Landlord -> {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        DividerWithSubhead(subhead = "Bank Information")
                        Spacer(modifier = Modifier.height(8.dp))

                        PersonalInformationCard(
                            infoMap = mapOf(
                                "Bank Code" to roleFormState.bankCode,
                                "Account Number" to roleFormState.bankAccountNumber
                            )
                        )
                    }
                }

                is RoleFormState.Tenant -> {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        DividerWithSubhead(subhead = "Additional Information")
                        Spacer(modifier = Modifier.height(8.dp))

                        PersonalInformationCard(
                            infoMap = mapOf(
                                "Occupation" to roleFormState.occupation,
                                "ID Card Number" to roleFormState.idCardNumber,
                                "ID Card Issue Date" to roleFormState.idCardIssueDate,
                                "Emergency Contact" to roleFormState.emergencyContact
                            )
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        DividerWithSubhead(subhead = "ID Card Images")
                        Spacer(modifier = Modifier.height(8.dp))

                        IDCardImages(
                            idCardFrontImageUrl = roleFormState.idCardImageFrontUrl,
                            idCardBackImageUrl = roleFormState.idCardImageBackUrl
                        )
                    }
                }

                else -> { /* no-op */
                }
            }
        }

        // Floating Action Button for Edit
        FloatingActionButton(
            onClick = onNavigateToEdit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit")
        }
    }
}
