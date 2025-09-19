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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.PasswordTextField
import com.example.baytro.viewModel.SignInVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInScreen(
    viewModel: SignInVM = koinViewModel(),
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val uiState by viewModel.signInState.collectAsState()
    val context = LocalContext.current

    SignInContent(
        uiState = uiState,
        onSignInClicked = { email, password ->
            viewModel.login(email, password)
        },
        onNavigateToSignUp = onNavigateToSignUp
    )

    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(context, "Đăng nhập thành công! Chào mừng ${state.user.email}", Toast.LENGTH_SHORT).show()
                onSignInSuccess()
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
    uiState: AuthUIState,
    onSignInClicked: (String, String) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            PasswordTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSignInClicked(email, password) },
                enabled = uiState !is AuthUIState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState is AuthUIState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Sign In")
                }
            }
            TextButton(onClick = onNavigateToSignUp) {
                Text("Don't have an account? Sign Up")
            }

        }
    }
}