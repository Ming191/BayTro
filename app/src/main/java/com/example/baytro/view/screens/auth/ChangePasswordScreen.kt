package com.example.baytro.view.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.baytro.auth.ChangePasswordFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.PasswordTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.viewModel.auth.ChangePasswordVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordVM = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSignOut: () -> Unit
) {
    val formState by viewModel.changePasswordFormState.collectAsState()
    val uiState by viewModel.changePasswordUIState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ChangePasswordContent(
            formState = formState,
            uiState = uiState,
            onPasswordChange = viewModel::onPasswordChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onConfirmNewPasswordChange = viewModel::onConfirmNewPasswordChange,
            onChangePasswordClicked = viewModel::changePassword,
            modifier = Modifier.padding(paddingValues)
        )
    }

    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is AuthUIState.PasswordChangedSuccess -> {
                Toast.makeText(
                    context,
                    "Change password success!",
                    Toast.LENGTH_LONG
                ).show()
                onNavigateToSignOut()
            }
            is AuthUIState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }
}

@Composable
fun ChangePasswordContent(
    formState: ChangePasswordFormState,
    uiState: AuthUIState,
    onPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onChangePasswordClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val passwordFocus = remember { FocusRequester() }
    val newPasswordFocus = remember { FocusRequester() }
    val confirmNewPasswordFocus = remember { FocusRequester() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            PasswordTextField(
                value = formState.password,
                onValueChange = onPasswordChange,
                label = "Password *",
                isError = formState.passwordError is ValidationResult.Error,
                errorMessage = formState.passwordError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onChangePasswordClicked() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocus)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            PasswordTextField(
                value = formState.newPassword,
                onValueChange = onNewPasswordChange,
                label = "New password *",
                isError = formState.newPasswordError is ValidationResult.Error|| formState.newPasswordStrengthError is ValidationResult.Error,
                errorMessage = formState.newPasswordError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onChangePasswordClicked() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(newPasswordFocus)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            PasswordTextField(
                value = formState.confirmNewPassword,
                onValueChange = onConfirmNewPasswordChange,
                label = "Confirm password *",
                isError = formState.confirmNewPasswordError is ValidationResult.Error,
                errorMessage = formState.confirmNewPasswordError.let {
                    if (it is ValidationResult.Error) it.message else null
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onChangePasswordClicked() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(confirmNewPasswordFocus)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SubmitButton(
                text = "Submit",
                isLoading = uiState is AuthUIState.Loading,
                onClick = onChangePasswordClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }
    }
}