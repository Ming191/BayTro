package com.example.baytro.view.screens.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.baytro.auth.SignInFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.SignInUiState
import com.example.baytro.view.components.PasswordTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.viewModel.auth.SignInVM
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.ui.platform.testTag

@Composable
fun SignInScreen(
    viewModel: SignInVM = koinViewModel(),
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onFirstTimeUser: () -> Unit,
    onTenantNoContract: () -> Unit,
    onTenantPendingSession: () -> Unit,
    onTenantWithContract: () -> Unit
) {
    val formState by viewModel.signInFormState.collectAsState()
    val loginUiState by viewModel.signInUIState.collectAsState()
    val context = LocalContext.current

    Log.d("SignInScreen", "Composing SignInScreen - Current state: ${loginUiState::class.simpleName}")

    SignInContent(
        formState = formState,
        loginUiState = loginUiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignInClicked = {
            Log.d("SignInScreen", "SignIn button clicked")
            viewModel.login()
        },
        onNavigateToSignUp = {
            Log.d("SignInScreen", "Navigate to SignUp clicked")
            onNavigateToSignUp()
        },
        onNavigateToForgotPassword = {
            Log.d("SignInScreen", "Navigate to ForgotPassword clicked")
            onNavigateToForgotPassword()
        }
    )
    LaunchedEffect(key1 = loginUiState) {
        Log.d("SignInScreen", "LaunchedEffect triggered - State: ${loginUiState::class.simpleName}")
//        viewModel.resetState()
        when (val state = loginUiState) {
            is SignInUiState.Success -> {
                Log.d("SignInScreen", "Success state - Navigating to main screen")
                onSignInSuccess()
            }
            is SignInUiState.TenantWithContract -> {
                Log.d("SignInScreen", "TenantWithContract state - Navigating to tenant dashboard")
                onTenantWithContract()
            }
            is SignInUiState.FirstTimeUser -> {
                Log.d("SignInScreen", "FirstTimeUser state - Navigating to onboarding")
                onFirstTimeUser()
            }
            is SignInUiState.TenantNoContract -> {
                Log.d("SignInScreen", "TenantNoContract state - Navigating to no contract screen")
                onTenantNoContract()
            }
            is SignInUiState.TenantPendingSession -> {
                Log.d("SignInScreen", "TenantPendingSession state - Navigating to pending session")
                onTenantPendingSession()
            }
            is SignInUiState.Error -> {
                Log.e("SignInScreen", "Error state: ${state.message}")
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            is SignInUiState.NeedVerification -> {
                Log.w("SignInScreen", "NeedVerification state: ${state.message}")
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            is SignInUiState.Loading -> {
                Log.d("SignInScreen", "Loading state")
            }
            is SignInUiState.Idle -> {
                Log.d("SignInScreen", "Idle state")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInContent(
    formState: SignInFormState,
    loginUiState: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClicked: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome Back!", style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(16.dp))

            RequiredTextField(
                value = formState.email,
                onValueChange = onEmailChange,
                label = "Email",
                isError = formState.emailError is ValidationResult.Error,
                errorMessage = formState.emailError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { passwordFocus.requestFocus() }
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(emailFocus).testTag("emailField")
            )

            PasswordTextField(
                value = formState.password,
                onValueChange = onPasswordChange,
                label = "Password",
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus).testTag("passwordField"),
                isError = formState.passwordError is ValidationResult.Error,
                errorMessage = formState.passwordError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onSignInClicked() }
                )
            )

            SubmitButton(
                text = "Sign In",
                isLoading = loginUiState is SignInUiState.Loading,
                onClick = onSignInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signInButton")
            )

            TextButton(onClick = onNavigateToForgotPassword) {
                Text(
                    text = "Forgot Password?",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            TextButton(onClick = onNavigateToSignUp) {
                Text("Don't have an account? Sign Up")
            }
        }

        Text(
            text = "Made with <3 for landlords & tenants © 2025 BayTro",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}