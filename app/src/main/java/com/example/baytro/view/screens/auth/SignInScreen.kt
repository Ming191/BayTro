package com.example.baytro.view.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.baytro.auth.SignInFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.PasswordTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.viewModel.auth.SignInVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(
    viewModel: SignInVM = koinViewModel(),
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onFirstTimeUser: () -> Unit,
    onTenantNoContract: () -> Unit,
    onTenantPendingSession: () -> Unit
) {
    val formState by viewModel.signInFormState.collectAsState()
    val loginUiState by viewModel.signInUIState.collectAsState()
    val context = LocalContext.current

    SignInContent(
        formState = formState,
        loginUiState = loginUiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignInClicked = viewModel::login,
        onNavigateToSignUp = onNavigateToSignUp
    )
    LaunchedEffect(key1 = loginUiState) {
        when (val state = loginUiState) {
            is AuthUIState.Success -> {
                onSignInSuccess()
            }
            is AuthUIState.FirstTimeUser -> {
                onFirstTimeUser()
            }
            is AuthUIState.TenantNoContract -> {
                onTenantNoContract()
            }
            is AuthUIState.TenantPendingSession -> {
                onTenantPendingSession()
            }
            is AuthUIState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            is AuthUIState.NeedVerification -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInContent(
    formState: SignInFormState,
    loginUiState: AuthUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClicked: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Welcome Back!", style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(16.dp))

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

            PasswordTextField(
                value = formState.password,
                onValueChange = onPasswordChange,
                label = "Password",
                modifier = Modifier.fillMaxWidth(),
                isError = formState.passwordError is ValidationResult.Error,
                errorMessage = formState.passwordError.let {
                    if (it is ValidationResult.Error) it.message else null
                }
            )

            SubmitButton(
                text = "Sign In",
                isLoading = loginUiState is AuthUIState.Loading,
                onClick = onSignInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            TextButton(onClick = onNavigateToSignUp) {
                Text("Don't have an account? Sign Up")
            }
        }
    }
}