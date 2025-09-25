package com.example.baytro.view.screens.splash

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.Logo
import com.example.baytro.viewModel.splash.SplashScreenVM
import com.example.baytro.viewModel.splash.SplashUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    navigateToLandlordLogin: () -> Unit,
    navigateToTenantLogin: () -> Unit,
    viewModel: SplashScreenVM = koinViewModel()
) {
    val splashUiState by viewModel.splashUiState.collectAsState()
    SplashScreenContent(
        onSplashCompleted = viewModel::onComplete
    )

    LaunchedEffect(key1 = splashUiState) {
        when (splashUiState) {
            is SplashUiState.TenantLogin -> {
                navigateToTenantLogin()
            }
            is SplashUiState.LandlordLogin -> {
                navigateToLandlordLogin()
            }
            else -> {}
        }
    }
}

@Composable
fun SplashScreenContent(
    onSplashCompleted: () -> Unit,
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
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
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
            Button(
                onClick = {
                    onSplashCompleted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Get Started")
            }
        }
    }
}