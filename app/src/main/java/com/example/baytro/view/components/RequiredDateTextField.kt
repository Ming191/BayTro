package com.example.baytro.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequiredDateTextField(
    modifier: Modifier = Modifier,
    label: String,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        val confirmEnabled by remember {
            derivedStateOf { datePickerState.selectedDateMillis != null }
        }

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                            onDateSelected(formattedDate)
                        }
                    },
                    enabled = confirmEnabled
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = modifier) {
        RequiredTextField(
            value = selectedDate,
            onValueChange = {},
            label = label,
            isError = isError,
            errorMessage = errorMessage,
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 8.dp, bottom = 16.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable { showDatePicker = true },
            color = Color.Transparent,
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun RequiredDateTextFieldPreview() {
    var date by remember { mutableStateOf("") }

    RequiredDateTextField(
        label = "Date of Birth",
        selectedDate = date,
        onDateSelected = { selectedDate ->
            date = selectedDate
        },
        isError = false,
        errorMessage = null
    )
}