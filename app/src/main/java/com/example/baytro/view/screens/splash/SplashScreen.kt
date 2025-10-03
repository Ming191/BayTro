package com.example.baytro.view.screens.splash

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.baytro.data.RoleType
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.Logo
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.splash.SplashFormState
import com.example.baytro.viewModel.splash.SplashScreenVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    navigateToLandlordLogin: () -> Unit,
    navigateToTenantLogin: () -> Unit,
    viewModel: SplashScreenVM = koinViewModel()
) {
    val splashUiState by viewModel.splashUiState.collectAsState()
    val formState by viewModel.splashFormState.collectAsState()
    val context = LocalContext.current

    SplashScreenContent(
        onSplashCompleted = viewModel::onComplete,
        formState = formState,
        onRoleChange = viewModel::onRoleChange,
        splashUiState = splashUiState,
    )

    LaunchedEffect(key1 = splashUiState) {
        when (val state = splashUiState) {
            is UiState.Success -> {
                when (state.data) {
                    RoleType.LANDLORD -> navigateToLandlordLogin()
                    RoleType.TENANT -> navigateToTenantLogin()
                }
            }
            is UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
}

@Composable
fun SplashScreenContent(
    onSplashCompleted: () -> Unit,
    formState : SplashFormState,
    onRoleChange: (RoleType) -> Unit,
    splashUiState: UiState<RoleType>,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .background(color = MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column (
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Logo()
            Spacer(modifier = Modifier.padding(16.dp))
            Text(
                "7Tro helps you manage your rental rooms, track expenses, and stay on top of payments easily.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge

            )
            Spacer(modifier = Modifier.padding(16.dp))
            DropdownSelectField(
                options = RoleType.entries.toList(),
                selectedOption = formState.role,
                modifier = Modifier.fillMaxWidth(),
                label = "You're a",
                onOptionSelected = {onRoleChange(it)},
                isLowerCased = true,
            )
            Spacer(modifier = Modifier.padding(16.dp))
            SubmitButton(
                text = "Continue",
                isLoading = splashUiState is UiState.Loading,
                onClick = onSplashCompleted,
            )
        }
    }
}

