package com.example.baytro.view.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.baytro.auth.SignUpFormState
import com.example.baytro.data.RoleType
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.ChoiceSelection
import com.example.baytro.view.components.PasswordStrengthIndicator
import com.example.baytro.view.components.PasswordTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.viewModel.SignUpVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignUpScreen(
    viewModel: SignUpVM = koinViewModel(),
    onNavigateToSignIn: () -> Unit
) {
    val formState by viewModel.signUpFormState.collectAsState()
    val uiState by viewModel.signUpUIState.collectAsState()
    val context = LocalContext.current

    SignUpContent(
        uiState = uiState,
        onSignUpClicked = {
            viewModel.signUp()
        },
        formState = formState,
        onNavigateToSignIn = onNavigateToSignIn,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onPhoneNumberChange = viewModel::onPhoneNumberChange,
        onRoleChange = viewModel::onRoleChange
    )

    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is AuthUIState.NeedVerification -> {
                Toast.makeText(
                    context,
                    "Success sign-up! Check your email for verification",
                    Toast.LENGTH_SHORT)
                    .show()
                onNavigateToSignIn()
            }
            is AuthUIState.Error -> {
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_SHORT)
                    .show()
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpContent(
    formState: SignUpFormState,
    uiState: AuthUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClicked: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onRoleChange: (RoleType) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Create an Account", style = MaterialTheme.typography.headlineLarge)

            // Email field with memoized error state
            val emailError = remember(formState.emailError) {
                if (formState.emailError is ValidationResult.Error) formState.emailError.message else null
            }
            RequiredTextField (
                value = formState.email,
                onValueChange = onEmailChange,
                label = "Email",
                isError = formState.emailError is ValidationResult.Error,
                errorMessage = emailError,
                modifier = Modifier.fillMaxWidth()
            )

            // Phone field with memoized error state
            val phoneError = remember(formState.phoneNumberError) {
                if (formState.phoneNumberError is ValidationResult.Error) formState.phoneNumberError.message else null
            }
            RequiredTextField (
                value = formState.phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = "Phone Number",
                isError = formState.phoneNumberError is ValidationResult.Error,
                errorMessage = phoneError,
                modifier = Modifier.fillMaxWidth()
            )

            // Role selection with memoized error state
            val roleError = remember(formState.roleError) {
                if (formState.roleError is ValidationResult.Error) formState.roleError.message else null
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                ChoiceSelection(
                    options = RoleType.entries,
                    selectedOption = formState.roleType,
                    onOptionSelected = onRoleChange,
                    isError = formState.roleError is ValidationResult.Error,
                    errorMessage = roleError,
                )
            }
            // Password field with optimized error handling
            Column(modifier = Modifier.fillMaxWidth()) {
                val passwordError = remember(formState.passwordError, formState.passwordStrengthError) {
                    when {
                        formState.passwordError is ValidationResult.Error -> formState.passwordError.message
                        formState.passwordStrengthError is ValidationResult.Error -> formState.passwordStrengthError.message
                        else -> null
                    }
                }

                PasswordTextField(
                    value = formState.password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    isError = formState.passwordError is ValidationResult.Error || formState.passwordStrengthError is ValidationResult.Error,
                    errorMessage = passwordError,
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(
                    visible = formState.password.isNotEmpty()
                ) {
                    PasswordStrengthIndicator(password = formState.password)
                }
            }
            val confirmPasswordError = remember(formState.confirmPasswordError) {
                when {
                    formState.confirmPasswordError is ValidationResult.Error -> formState.confirmPasswordError.message
                    else -> null
                }
            }

            PasswordTextField(
                value = formState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm Password",
                isError = formState.confirmPasswordError is ValidationResult.Error,
                errorMessage = confirmPasswordError,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSignUpClicked,
                enabled = uiState !is AuthUIState.Loading,
                modifier = Modifier.fillMaxWidth()
                    .requiredHeight(50.dp)

            ) {
                if (uiState is AuthUIState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.wrapContentHeight().padding(4.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign Up")
                }
            }

            TextButton(onClick = onNavigateToSignIn) {
                Text("Already have an account? Sign In")
            }
        }
    }
}
