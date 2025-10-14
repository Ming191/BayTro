package com.example.baytro.view.screens.building

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.importExcel.ImportBuildingRoomVM
import com.example.baytro.viewModel.importExcel.ImportResult
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportBuildingsRoomsScreen(
    navController: NavHostController? = null,
    viewModel: ImportBuildingRoomVM = koinViewModel()
) {
    val uiState by viewModel.importState.collectAsState()
    val summary by viewModel.summary.collectAsState()
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileName = uri.lastPathSegment
            viewModel.startImport(uri)
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Import Buildings & Rooms",
                    style = MaterialTheme.typography.headlineSmall
                )

                Button(onClick = { filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }) {
                    Text(text = selectedFileName ?: "Choose Excel (.xlsx)")
                }

                when (val state = uiState) {
                    is UiState.Idle -> Text("Awaiting file selection")
                    is UiState.Loading -> CircularProgressIndicator()
                    is UiState.Error -> Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    is UiState.Success<*> -> {
                        val result = state.data as ImportResult
                        Text("Buildings: ${result.buildingsImported} imported, ${result.buildingsUpdated} updated, ${result.buildingsFailed} failed")
                        Text("Rooms: ${result.roomsImported} imported, ${result.roomsUpdated} updated, ${result.roomsFailed} failed")
                    }
                    else -> Unit
                }

                summary?.let { lines ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        lines.take(10).forEach { line -> Text(line) }
                        if (lines.size > 10) Text("...")
                    }
                }
            }
        }
    }
}