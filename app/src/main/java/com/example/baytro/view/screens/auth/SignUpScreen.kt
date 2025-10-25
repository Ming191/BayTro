package com.example.baytro.view.screens.auth

import android.util.Log
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.baytro.auth.SignUpFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.SignUpUiState
import com.example.baytro.view.components.PasswordStrengthIndicator
import com.example.baytro.view.components.PasswordTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.viewModel.auth.SignUpVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignUpScreen(
    viewModel: SignUpVM = koinViewModel(),
    onNavigateToSignIn: () -> Unit
) {
    val formState by viewModel.signUpFormState.collectAsState()
    val uiState by viewModel.signUpUIState.collectAsState()
    val context = LocalContext.current

    Log.d("SignUpScreen", "Composing SignUpScreen - Current state: ${uiState::class.simpleName}")

    // Reset state when leaving the screen
    DisposableEffect(Unit) {
        Log.d("SignUpScreen", "DisposableEffect - SignUpScreen entered")
        onDispose {
            Log.d("SignUpScreen", "DisposableEffect.onDispose - Resetting state")
            viewModel.resetState()
        }
    }

    SignUpContent(
        uiState = uiState,
        onSignUpClicked = {
            Log.d("SignUpScreen", "SignUp button clicked")
            viewModel.signUp()
        },
        formState = formState,
        onNavigateToSignIn = onNavigateToSignIn,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
    )

    LaunchedEffect(key1 = uiState) {
        Log.d("SignUpScreen", "LaunchedEffect triggered - State: ${uiState::class.simpleName}")
        when (val state = uiState) {
            is SignUpUiState.NeedVerification -> {
                Log.d("SignUpScreen", "NeedVerification state - Showing toast and navigating to SignIn")
                Toast.makeText(
                    context,
                    "Success sign-up! Check your email for verification",
                    Toast.LENGTH_SHORT)
                    .show()
                Log.d("SignUpScreen", "Calling onNavigateToSignIn()")
                onNavigateToSignIn()
                Log.d("SignUpScreen", "Navigation callback completed")
            }
            is SignUpUiState.Error -> {
                Log.e("SignUpScreen", "Error state: ${state.message}")
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_SHORT)
                    .show()
            }
            is SignUpUiState.Loading -> {
                Log.d("SignUpScreen", "Loading state")
            }
            is SignUpUiState.Idle -> {
                Log.d("SignUpScreen", "Idle state")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpContent(
    formState: SignUpFormState,
    uiState: SignUpUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClicked: () -> Unit,
    onNavigateToSignIn: () -> Unit,
) {
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val confirmPasswordFocus = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Create an Account", style = MaterialTheme.typography.headlineLarge)

            val emailError = remember(formState.emailError) {
                if (formState.emailError is ValidationResult.Error) formState.emailError.message else null
            }
            RequiredTextField (
                value = formState.email,
                onValueChange = onEmailChange,
                label = "Email",
                isError = formState.emailError is ValidationResult.Error,
                errorMessage = emailError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { passwordFocus.requestFocus() }
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(emailFocus)
            )
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { confirmPasswordFocus.requestFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus)
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onSignUpClicked() }
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(confirmPasswordFocus)
            )

            Button(
                onClick = onSignUpClicked,
                enabled = uiState !is SignUpUiState.Loading,
                modifier = Modifier.fillMaxWidth()
                    .requiredHeight(50.dp)

            ) {
                if (uiState is SignUpUiState.Loading) {
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
