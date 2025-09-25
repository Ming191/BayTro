package com.example.baytro.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.RoleType

@Composable
fun<T : Enum<T>> ChoiceSelection(
    options: List<T>,
    selectedOption: T? = null,
    onOptionSelected: (T) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var internalSelectedOption by remember { mutableStateOf(selectedOption) }

    Column {
        Text(
            text = "Select Role",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = internalSelectedOption == option

                val textColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    label = "textColor"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    label = "scale"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            internalSelectedOption = option
                            onOptionSelected(option)
                        }
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            internalSelectedOption = option
                            onOptionSelected(option)
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        ),
                    )
                    Text(
                        text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        AnimatedVisibility(visible = isError && !errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp)
            )
        }
    }
}

@Preview
@Composable
fun ChoiceSelectionPreview() {
    ChoiceSelection(
        options = RoleType.entries,
        selectedOption = null,
        onOptionSelected = {},
        isError = true,
        errorMessage = "Please select a role"
    )
}