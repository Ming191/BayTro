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
import com.example.baytro.viewModel.SignUpVM
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun SignUpScreen(
    viewModel: SignUpVM = koinViewModel(),
    onNavigateToSignIn: () -> Unit
) {
    val uiState by viewModel.signUpUIState.collectAsState()
    val context = LocalContext.current

    SignUpContent(
        uiState = uiState,
        onSignUpClicked = { email, password, confirmPassword ->
//            viewModel.signUp(email, password, confirmPassword)
        },
        onNavigateToSignIn = onNavigateToSignIn
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
    uiState: AuthUIState,
    onSignUpClicked: (String, String, String) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Create an Account", style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

//            PasswordTextField(
//                value = password,
//                onValueChange = { password = it },
//                label = "Password",
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            PasswordTextField(
//                value = confirmPassword,
//                onValueChange = { confirmPassword = it },
//                label = "Confirm Password",
//                modifier = Modifier.fillMaxWidth()
//            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSignUpClicked(email, password, confirmPassword) },
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