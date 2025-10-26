// SettingsScreen.kt

package com.example.baytro.view

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.baytro.viewModel.PrefixCheckState
import com.example.baytro.viewModel.SettingsUserEvent
import com.example.baytro.viewModel.SettingsVM
import org.koin.compose.viewmodel.koinViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(
    viewModel: SettingsVM = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.errorEvent) {
        uiState.errorEvent?.getContentIfNotHandled()?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.onEvent(SettingsUserEvent.OnErrorShown)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val isSaveEnabled = (
                    (uiState.prefixCheckState is PrefixCheckState.Available ||
                            uiState.prefix.equals(uiState.originalPrefix, ignoreCase = true)) &&
                            uiState.prefix.isNotBlank() &&
                            uiState.suffixLength.toIntOrNull() in 4..8
                    )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AnimatedSection(delayMillis = 0) {
                    InstructionsBanner()
                }

                AnimatedSection(delayMillis = 100) {
                    ConfigurationProgressCard(
                        hasValidPrefix = (uiState.prefixCheckState is PrefixCheckState.Available ||
                                (uiState.prefix.isNotBlank() && uiState.prefix.equals(uiState.originalPrefix, ignoreCase = true))),
                        hasValidSuffix = uiState.suffixLength.toIntOrNull() in 4..8
                    )
                }

                AnimatedSection(delayMillis = 200) {
                    PrefixConfigurationCard(
                        prefix = uiState.prefix,
                        prefixState = uiState.prefixCheckState,
                        onPrefixChange = { newPrefix ->
                            viewModel.onEvent(SettingsUserEvent.OnPrefixChange(newPrefix))
                        }
                    )
                }

                AnimatedSection(delayMillis = 300) {
                    SuffixConfigurationCard(
                        suffixLength = uiState.suffixLength,
                        onSuffixChange = { newLength ->
                            viewModel.onEvent(SettingsUserEvent.OnSuffixLengthChange(newLength))
                        }
                    )
                }

                AnimatedSection(delayMillis = 400) {
                    ExamplePreviewCard(uiState.prefix, uiState.suffixLength)
                }

                AnimatedSection(delayMillis = 500) {
                    SaveButton(
                        enabled = isSaveEnabled,
                        isSaving = uiState.isSaving,
                        onClick = { viewModel.onEvent(SettingsUserEvent.OnSaveClick) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp
        )
    ) {
        AnimatedContent(
            targetState = isSaving,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "buttonText"
        ) { saving ->
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                // Giữ nguyên text gốc của bạn, chỉ thay đổi logic lấy text một chút
                val text = if (enabled) "Save Configuration" else "Complete All Fields"
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AnimatedSection(
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 500, easing = EaseOutCubic)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
            initialOffsetY = { it / 3 }
        )
    ) {
        content()
    }
}

@Composable
private fun InstructionsBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(scale)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Important",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Choose a unique prefix and configure suffix length for your payment codes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ConfigurationProgressCard(
    hasValidPrefix: Boolean,
    hasValidSuffix: Boolean
) {
    val completedSteps = listOf(hasValidPrefix, hasValidSuffix).count { it }
    val totalSteps = 2
    val targetProgress = completedSteps.toFloat() / totalSteps

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configuration Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                AnimatedContent(
                    targetState = completedSteps,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { -it } + fadeIn()).togetherWith(
                                slideOutVertically { it } + fadeOut()
                            )
                        } else {
                            (slideInVertically { it } + fadeIn()).togetherWith(
                                slideOutVertically { -it } + fadeOut()
                            )
                        }.using(SizeTransform(clip = false))
                    },
                    label = "stepCounter"
                ) { steps ->
                    Text(
                        text = "$steps / $totalSteps",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressCheckItem(
                    label = "Valid prefix",
                    completed = hasValidPrefix,
                    modifier = Modifier.weight(1f)
                )
                ProgressCheckItem(
                    label = "Valid suffix length",
                    completed = hasValidSuffix,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProgressCheckItem(
    label: String,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkScale"
    )

    Row(
        modifier = modifier.scale(scale),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = completed,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn() togetherWith scaleOut() + fadeOut()
            },
            label = "checkIcon"
        ) { isCompleted ->
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (completed)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PrefixConfigurationCard(
    prefix: String,
    prefixState: PrefixCheckState,
    onPrefixChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prefix Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "Required",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            OutlinedTextField(
                value = prefix,
                onValueChange = onPrefixChange,
                label = { Text("Payment Prefix") },
                placeholder = { Text("e.g., BAYTRO") },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                isError = prefixState is PrefixCheckState.Taken || prefixState is PrefixCheckState.Error,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = when (prefixState) {
                        is PrefixCheckState.Available -> MaterialTheme.colorScheme.primary
                        is PrefixCheckState.Taken, is PrefixCheckState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                ),
                trailingIcon = { PrefixStatusIcon(prefixState) },
                supportingText = {
                    PrefixSupportingText(prefixState, prefix)
                }
            )
        }
    }
}

@Composable
private fun SuffixConfigurationCard(
    suffixLength: String,
    onSuffixChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Suffix Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "Required",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = "Set the length of the random suffix (4-8 characters)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = suffixLength,
                onValueChange = onSuffixChange,
                label = { Text("Suffix Length") },
                placeholder = { Text("6") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = suffixLength.toIntOrNull() !in 4..8,
                supportingText = {
                    val length = suffixLength.toIntOrNull()
                    Text(
                        text = when {
                            length == null || length !in 4..8 -> "Please enter a value between 4 and 8"
                            else -> "Valid length: generates ${length} random characters"
                        },
                        color = if (length in 4..8)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

@Composable
private fun ExamplePreviewCard(prefix: String, suffixLength: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Preview Example",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val exampleSuffix = "ABC123XYZ".takeLast(suffixLength.toIntOrNull() ?: 6).uppercase()
            val fullCode = if (prefix.isNotBlank()) "${prefix}${exampleSuffix}" else "---"

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fullCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = MaterialTheme.typography.headlineLarge.letterSpacing * 1.5,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PrefixStatusIcon(state: PrefixCheckState) {
    AnimatedVisibility(
        visible = state !is PrefixCheckState.Idle,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut()
    ) {
        when (state) {
            is PrefixCheckState.Checking -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is PrefixCheckState.Available -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Prefix available",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            is PrefixCheckState.Taken, is PrefixCheckState.Error -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Prefix unavailable",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun PrefixSupportingText(state: PrefixCheckState, prefix: String) {
    val text = when (state) {
        is PrefixCheckState.Checking -> "Checking availability..."
        is PrefixCheckState.Available -> "This prefix is available"
        is PrefixCheckState.Taken -> "This prefix is already in use"
        is PrefixCheckState.Error -> state.message
        is PrefixCheckState.Idle -> if (prefix.isBlank()) "Enter a unique prefix" else ""
    }

    val color = when (state) {
        is PrefixCheckState.Available -> MaterialTheme.colorScheme.primary
        is PrefixCheckState.Taken, is PrefixCheckState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AnimatedVisibility(visible = text.isNotBlank()) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall
        )
    }
}