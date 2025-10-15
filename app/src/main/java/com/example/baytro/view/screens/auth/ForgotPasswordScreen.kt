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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.baytro.auth.ForgotPasswordFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.viewModel.auth.ForgotPasswordVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordVM = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val formState by viewModel.forgotPasswordFormState.collectAsState()
    val uiState by viewModel.forgotPasswordUIState.collectAsState()
    val context = LocalContext.current

    Scaffold { paddingValues ->
        ForgotPasswordContent(
            formState = formState,
            uiState = uiState,
            onEmailChange = viewModel::onEmailChange,
            onResetPasswordClicked = viewModel::resetPassword,
            onNavigateToSignIn = onNavigateToSignIn,
            modifier = Modifier.padding(paddingValues)
        )
    }

    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is AuthUIState.PasswordResetSuccess -> {
                Toast.makeText(
                    context,
                    "Password reset email sent! Check your inbox.",
                    Toast.LENGTH_LONG
                ).show()
            }
            is AuthUIState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }
}

@Composable
fun ForgotPasswordContent(
    formState: ForgotPasswordFormState,
    uiState: AuthUIState,
    onEmailChange: (String) -> Unit,
    onResetPasswordClicked: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emailFocus = remember { FocusRequester() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Forgot Password?",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your email address and we'll send you a link to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            RequiredTextField(
                value = formState.email,
                onValueChange = onEmailChange,
                label = "Email",
                isError = formState.emailError is ValidationResult.Error,
                errorMessage = formState.emailError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onResetPasswordClicked() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocus)
            )

            Spacer(modifier = Modifier.height(24.dp))

            SubmitButton(
                text = "Send reset email",
                isLoading = uiState is AuthUIState.Loading,
                onClick = onResetPasswordClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToSignIn) {
                Text("Back to Sign In")
            }
        }
    }
}
