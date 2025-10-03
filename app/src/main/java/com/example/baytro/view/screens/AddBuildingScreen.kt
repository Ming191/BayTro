package com.example.baytro.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.building.Building
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.viewModel.AddBuildingVM
import org.koin.compose.viewmodel.koinViewModel
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBuildingScreen(
    navController: NavHostController? = null,
    viewModel: AddBuildingVM = koinViewModel()
) {
    val uiState by viewModel.addBuildingUIState.collectAsState()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var statusExpanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Active") }
    var billingDate by remember { mutableStateOf("") }
    var paymentStart by remember { mutableStateOf("") }
    var paymentDue by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var floorError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var billingError by remember { mutableStateOf(false) }
    var startError by remember { mutableStateOf(false) }
    var dueError by remember { mutableStateOf(false) }

    var nameErrorMsg by remember { mutableStateOf<String?>(null) }
    var floorErrorMsg by remember { mutableStateOf<String?>(null) }
    var addressErrorMsg by remember { mutableStateOf<String?>(null) }
    var billingErrorMsg by remember { mutableStateOf<String?>(null) }
    var startErrorMsg by remember { mutableStateOf<String?>(null) }
    var dueErrorMsg by remember { mutableStateOf<String?>(null) }

    val nameFocus = remember { FocusRequester() }
    val floorFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val billingFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val dueFocus = remember { FocusRequester() }

    // Handle UI state changes
    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(
                    context,
                    "Building added successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                navController?.popBackStack()
            }
            is AuthUIState.Error -> {
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> Unit
        }
    }

    fun validateName(): Pair<Boolean, String?> {
        val invalid = name.isBlank()
        return invalid to if (invalid) "Building name is required" else null
    }

    fun validateAddress(): Pair<Boolean, String?> {
        val invalid = address.isBlank()
        return invalid to if (invalid) "Address is required" else null
    }

    fun validateFloor(): Pair<Boolean, String?> {
        if (floor.isBlank()) return true to "Floor is required"
        val value = floor.toIntOrNull()
        if (value == null) return true to "Floor must be a positive integer"
        if (value <= 0) return true to "Floor must be a positive integer"
        return false to null
    }

    fun validateBilling(): Pair<Boolean, String?> {
        if (billingDate.isBlank()) return true to "Billing date is required"
        val value = billingDate.toIntOrNull()
        if (value == null) return true to "Billing date must be a positive integer"
        if (value <= 0) return true to "Billing date must be a positive integer"
        return false to null
    }

    fun validateStart(): Pair<Boolean, String?> {
        if (paymentStart.isBlank()) return true to "Payment start is required"
        val value = paymentStart.toIntOrNull()
        if (value == null) return true to "Payment start must be a positive integer"
        if (value <= 0) return true to "Payment start must be a positive integer"
        return false to null
    }

    fun validateDue(): Pair<Boolean, String?> {
        if (paymentDue.isBlank()) return true to "Payment due is required"
        val value = paymentDue.toIntOrNull()
        if (value == null) return true to "Payment due must be a positive integer"
        if (value <= 0) return true to "Payment due must be a positive integer"
        return false to null
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            RequiredTextField(
                value = name,
                onValueChange = {
                    name = it
                    validateName().also { (err, msg) -> nameError = err; nameErrorMsg = msg }
                },
                label = "Building name",
                isError = nameError,
                errorMessage = nameErrorMsg,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = floor,
                onValueChange = { newValue ->
                    floor = newValue
                    validateFloor().also { (err, msg) -> floorError = err; floorErrorMsg = msg }
                },
                label = { Text("Floor") },
                singleLine = true,
                isError = floorError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (floorError) Text(text = floorErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth()
            )

            RequiredTextField(
                value = address,
                onValueChange = {
                    address = it
                    validateAddress().also { (err, msg) ->
                        addressError = err; addressErrorMsg = msg
                    }
                },
                label = "Address",
                isError = addressError,
                errorMessage = addressErrorMsg,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded }) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                DropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }) {
                    listOf("Active", "Inactive").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            status = option
                            statusExpanded = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = billingDate,
                onValueChange = { newValue ->
                    billingDate = newValue
                    validateBilling().also { (err, msg) ->
                        billingError = err; billingErrorMsg = msg
                    }
                },
                label = { Text("Billing date") },
                singleLine = true,
                isError = billingError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (billingError) Text(text = billingErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = paymentStart,
                        onValueChange = { newValue ->
                            paymentStart = newValue
                            validateStart().also { (err, msg) ->
                                startError = err; startErrorMsg = msg
                            }
                        },
                        label = { Text("Payment start") },
                        singleLine = true,
                        isError = startError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { if (startError) Text(text = startErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = paymentDue,
                        onValueChange = { newValue ->
                            paymentDue = newValue
                            validateDue().also { (err, msg) -> dueError = err; dueErrorMsg = msg }
                        },
                        label = { Text("Payment due") },
                        singleLine = true,
                        isError = dueError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { if (dueError) Text(text = dueErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    if (uiState is AuthUIState.Loading) return@Button
                    // Reset all errors before progressive check
                    nameError = false; nameErrorMsg = null
                    floorError = false; floorErrorMsg = null
                    addressError = false; addressErrorMsg = null
                    billingError = false; billingErrorMsg = null
                    startError = false; startErrorMsg = null
                    dueError = false; dueErrorMsg = null

                    // Find first invalid field in order
                    val checks = listOf(
                        "name" to validateName(),
                        "floor" to validateFloor(),
                        "address" to validateAddress(),
                        "billing" to validateBilling(),
                        "start" to validateStart(),
                        "due" to validateDue(),
                    )
                    val firstInvalid = checks.firstOrNull { it.second.first }

                    if (firstInvalid != null) {
                        when (firstInvalid.first) {
                            "name" -> {
                                nameError = true; nameErrorMsg =
                                    firstInvalid.second.second;  nameFocus.requestFocus()
                            }

                            "floor" -> {
                                floorError = true; floorErrorMsg =
                                    firstInvalid.second.second;  floorFocus.requestFocus()
                            }

                            "address" -> {
                                addressError = true; addressErrorMsg =
                                    firstInvalid.second.second;  addressFocus.requestFocus()
                            }

                            "billing" -> {
                                billingError = true; billingErrorMsg =
                                    firstInvalid.second.second;  billingFocus.requestFocus()
                            }

                            "start" -> {
                                startError = true; startErrorMsg =
                                    firstInvalid.second.second;  startFocus.requestFocus()
                            }

                            "due" -> {
                                dueError = true; dueErrorMsg =
                                    firstInvalid.second.second; dueFocus.requestFocus()
                            }
                        }
                    }

                    val allGood = firstInvalid == null
                    if (allGood) {
                        val floorInt = floor.toInt()
                        val billingDateInt = billingDate.toInt()
                        val paymentStartInt = paymentStart.toInt()
                        val paymentDueInt = paymentDue.toInt()
                        val building = Building(
                            name = name,
                            floor = floorInt,
                            address = address,
                            status = status,
                            billingDate = billingDateInt,
                            paymentStart = paymentStartInt,
                            paymentDue = paymentDueInt,
                        )
                        viewModel.addBuilding(building)
                    }
                },
                modifier = Modifier.fillMaxWidth().requiredHeight(50.dp),
                enabled = uiState !is AuthUIState.Loading
            ) {
                if (uiState is AuthUIState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Text("Confirm")
                }
            }
        }
    }
}




