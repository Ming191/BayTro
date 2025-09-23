package com.example.baytro.view.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.auth.SignUpFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.Logo
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
        onSignUpClicked = { email, password, confirmPassword ->
            viewModel.signUp()
        },
        formState = formState,
        onNavigateToSignIn = onNavigateToSignIn,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange
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

@Preview
@Composable
fun SignUpContentPreview() {
    var formState by remember { mutableStateOf(SignUpFormState()) }
    SignUpContent(
        formState = formState,
        uiState = AuthUIState.Idle,
        onEmailChange = { formState = formState.copy(email = it) },
        onPasswordChange = { formState = formState.copy(password = it) },
        onConfirmPasswordChange = { formState = formState.copy(confirmPassword = it) },
        onSignUpClicked = { _, _, _ -> },
        onNavigateToSignIn = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpContent(
    formState: SignUpFormState,
    uiState: AuthUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClicked: (String, String, String) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Logo(
                modifier = Modifier.height(200.dp).wrapContentHeight(align = Alignment.Top)
            )

            Text("Create an Account", style = MaterialTheme.typography.headlineLarge)

            RequiredTextField (
                value = formState.email,
                onValueChange = onEmailChange,
                label = "Email",
                isError = formState.emailError is ValidationResult.Error,
                errorMessage = formState.emailError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column (
                modifier = Modifier.fillMaxWidth()
            ) {
                PasswordTextField(
                    value = formState.password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    isError = formState.passwordError is ValidationResult.Error || formState.passwordStrengthError is ValidationResult.Error,
                    errorMessage = formState.passwordError.let {
                        if (it is ValidationResult.Error) it.message else formState.passwordStrengthError.let { err -> null}
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(
                    visible = formState.password.isNotEmpty()
                ) {
                    PasswordStrengthIndicator(password = formState.password)
                }
            }
            PasswordTextField(
                value = formState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm Password",
                isError = formState.confirmPasswordError is ValidationResult.Error || formState.passwordMatchError is ValidationResult.Error,
                errorMessage = formState.confirmPasswordError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSignUpClicked(formState.email, formState.password, formState.confirmPassword) },
                enabled = uiState !is AuthUIState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState is AuthUIState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
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