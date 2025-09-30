import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.baytro.view.screens.UiState
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

@Composable
fun QrCodeDialog(
    state: UiState<String>,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
) {
    if (state !is UiState.Idle) {
        Dialog(onDismissRequest = onDismissRequest) {
            Card(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (state) {
                        is UiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generating code...")
                        }
                        // Tạm thời để trống
                        is UiState.Success -> {
                            QrCodeDisplayWithActions(sessionId = state.data)
                        }
                        is UiState.Error -> {
                            ErrorDisplay(message = state.message, onRetry)
                        }
                        is UiState.Idle -> { }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCodeDisplayWithActions(sessionId: String) {
    val context = LocalContext.current

    val qrBitmap = remember(sessionId) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(sessionId, BarcodeFormat.QR_CODE, 500, 500)
        } catch (e: Exception) { null }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Ask the tenant to scan this code", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text("This code is valid for 5 minutes.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        qrBitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(250.dp))
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            qrBitmap?.let { bitmap ->
                // shareBitmap(context, bitmap, "Share QR Code")
            }
        }) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share")
        }
    }
}

// Composable để hiển thị lỗi
@Composable
private fun ErrorDisplay(message: String, onRetryClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("An Error Occurred", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text("TRY AGAIN")
        }
    }
}