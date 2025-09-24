package com.example.baytro.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.io.Serializable

data class Building(
    val name: String,
    val floor: Int,
    val address: String,
    val status: String,
    val billingDate: Int,
    val paymentStart: Int,
    val paymentDue: Int,
) : Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBuildingScreen(
    navController: NavHostController? = null
) {
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

    var nameTouched by remember { mutableStateOf(false) }
    var floorTouched by remember { mutableStateOf(false) }
    var addressTouched by remember { mutableStateOf(false) }
    var billingTouched by remember { mutableStateOf(false) }
    var startTouched by remember { mutableStateOf(false) }
    var dueTouched by remember { mutableStateOf(false) }

    val nameFocus = remember { FocusRequester() }
    val floorFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val billingFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val dueFocus = remember { FocusRequester() }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Variables.SchemesSurface, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))


        Text(text = "Building name *", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameTouched = true
                validateName().also { (err, msg) -> nameError = err; nameErrorMsg = msg }
            },
            label = { },
            isError = nameError,
            supportingText = { if (nameError) Text(text = nameErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocus)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Floor *", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = floor,
            onValueChange = {
                floor = it
                floorTouched = true
                validateFloor().also { (err, msg) -> floorError = err; floorErrorMsg = msg }
            },
            label = { },
            isError = floorError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = { if (floorError) Text(text = floorErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(floorFocus)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Address *", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = address,
            onValueChange = {
                address = it
                addressTouched = true
                validateAddress().also { (err, msg) -> addressError = err; addressErrorMsg = msg }
            },
            label = { },
            isError = addressError,
            supportingText = { if (addressError) Text(text = addressErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(addressFocus)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Status", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }) {
            OutlinedTextField(
                value = status,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                listOf("Active", "Inactive").forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        status = option
                        statusExpanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Billing date *", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = billingDate,
            onValueChange = {
                billingDate = it
                billingTouched = true
                validateBilling().also { (err, msg) -> billingError = err; billingErrorMsg = msg }
            },
            label = { },
            isError = billingError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = { if (billingError) Text(text = billingErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(billingFocus)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Payment start *", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                value = paymentStart,
                onValueChange = {
                    paymentStart = it
                    startTouched = true
                    validateStart().also { (err, msg) -> startError = err; startErrorMsg = msg }
                },
                label = { },
                isError = startError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (startError) Text(text = startErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(startFocus)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Payment due *", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                value = paymentDue,
                onValueChange = {
                    paymentDue = it
                    dueTouched = true
                    validateDue().also { (err, msg) -> dueError = err; dueErrorMsg = msg }
                },
                label = { },
                isError = dueError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (dueError) Text(text = dueErrorMsg ?: "", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(dueFocus)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
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
                            "name" -> { nameError = true; nameErrorMsg = firstInvalid.second.second; nameTouched = true; nameFocus.requestFocus() }
                            "floor" -> { floorError = true; floorErrorMsg = firstInvalid.second.second; floorTouched = true; floorFocus.requestFocus() }
                            "address" -> { addressError = true; addressErrorMsg = firstInvalid.second.second; addressTouched = true; addressFocus.requestFocus() }
                            "billing" -> { billingError = true; billingErrorMsg = firstInvalid.second.second; billingTouched = true; billingFocus.requestFocus() }
                            "start" -> { startError = true; startErrorMsg = firstInvalid.second.second; startTouched = true; startFocus.requestFocus() }
                            "due" -> { dueError = true; dueErrorMsg = firstInvalid.second.second; dueTouched = true; dueFocus.requestFocus() }
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
                        navController?.previousBackStackEntry?.savedStateHandle?.set("new_building", building)
                        navController?.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm")
            }
        }
    }
}




