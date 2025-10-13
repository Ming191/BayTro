package com.example.baytro.view.screens.contract

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.contract.TenantJoinVM
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.koin.androidx.compose.koinViewModel

@Composable
fun TenantEmptyContractView(
    viewModel: TenantJoinVM = koinViewModel(),
    onContractConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    Log.d("TenantEmptyContract", "Current state: $state")

    LaunchedEffect(state) {
        when (state) {
            is UiState.Success -> {
                Log.d("TenantEmptyContract", "Success state triggered")
                Toast.makeText(context, (state as UiState.Success<String>).data, Toast.LENGTH_LONG).show()
                onContractConfirmed()
                viewModel.clearState()
            }
            is UiState.Error -> {
                Log.d("TenantEmptyContract", "Error state triggered: ${(state as UiState.Error).message}")
                Toast.makeText(context, (state as UiState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.clearState()
            }
            is UiState.Waiting -> {
                Log.d("TenantEmptyContract", "Waiting state - should show waiting screen")
            }
            is UiState.Loading -> {
                Log.d("TenantEmptyContract", "Loading state - checking for pending session")
            }
            is UiState.Idle -> {
                Log.d("TenantEmptyContract", "Idle state - should show QR scan interface")
            }
        }
    }

    when (state) {
        is UiState.Waiting -> {
            Log.d("TenantEmptyContract", "Rendering waiting screen")
            TenantWaitingScreen(
                onCancel = {
                    Log.d("TenantEmptyContract", "Cancel button clicked in waiting screen")
                    viewModel.clearState()
                }
            )
        }
        is UiState.Loading -> {
            Log.d("TenantEmptyContract", "Rendering loading screen")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            Log.d("TenantEmptyContract", "Rendering QR scan interface")
            QrScanInterface(viewModel = viewModel, state = state)
        }
    }
}

@Composable
private fun QrScanInterface(
    viewModel: TenantJoinVM,
    state: UiState<String>
) {
    val context = LocalContext.current
    var showOptionsDialog by remember { mutableStateOf(false) }

    Log.d("TenantEmptyContract", "QrScanInterface composed with showOptionsDialog: $showOptionsDialog")

    // Create launchers only in this composable
    val cameraScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        Log.d("TenantEmptyContract", "Camera scanner result: ${result.contents}")

        if (result.contents != null && result.contents.isNotBlank()) {
            Log.d("TenantEmptyContract", "Valid QR code scanned: ${result.contents}")
            viewModel.processQrScan(result.contents)
        } else {
            Log.w("TenantEmptyContract", "Camera scanner returned null or empty result")
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("TenantEmptyContract", "Gallery picker result: $uri")
        uri?.let { viewModel.processQrFromUri(context, it) }
    }

    TenantEmptyContractContent(
        state = state,
        onScanQrClick = {
            Log.d("TenantEmptyContract", "Scan QR button clicked")
            showOptionsDialog = true
        }
    )

    if (showOptionsDialog) {
        Log.d("TenantEmptyContract", "Showing options dialog")
        AlertDialog(
            onDismissRequest = {
                Log.d("TenantEmptyContract", "Options dialog dismissed")
                showOptionsDialog = false
            },
            title = { Text("Join Contract") },
            text = { Text("How would you like to add the contract?") },
            confirmButton = {
                TextButton(onClick = {
                    Log.d("TenantEmptyContract", "Camera option selected")
                    showOptionsDialog = false
                    val options = ScanOptions().apply {
                        setPrompt("Scan the QR code on the landlord's phone")
                        setBeepEnabled(true)
                        setOrientationLocked(true)
                        setCameraId(0) // Use back camera
                        setBarcodeImageEnabled(true)
                        setTimeout(30000) // 30 second timeout
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity::class.java)
                    }
                    Log.d("TenantEmptyContract", "Launching camera with options: $options")
                    cameraScannerLauncher.launch(options)
                }) { Text("Use Camera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    Log.d("TenantEmptyContract", "Gallery option selected")
                    showOptionsDialog = false
                    galleryPickerLauncher.launch("image/*")
                }) { Text("Select from Gallery") }
            }
        )
    }
}

@Composable
fun TenantEmptyContractContent(
    state: UiState<String>,
    onScanQrClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Scan QR Code",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Headline
        Text(
            text = "You haven't joined a tenancy",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scan the QR code provided by your landlord to be added to the contract and manage your tenancy details.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        SubmitButton(
            text = "Scan or Upload QR Code",
            onClick = onScanQrClick,
            isLoading = state is UiState.Loading,
        )
    }
}

@Composable
fun TenantWaitingScreen(
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "QR Scan Successful",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "QR Code Scanned Successfully!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please wait while your landlord approves your request to join the contract.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Waiting for landlord approval...",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onCancel
        ) {
            Text("Cancel")
        }
    }
}
