package com.example.baytro.view.screens.profile

import android.annotation.SuppressLint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.baytro.view.SettingsScreen
import com.example.baytro.viewModel.SettingsVM
import org.koin.compose.viewmodel.koinViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSettingsScreen(
    viewModel: SettingsVM = koinViewModel()
) {
    SettingsScreen(viewModel = viewModel)
}

