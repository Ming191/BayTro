package com.example.baytro.view.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun PasswordStrengthIndicatorPreview() {
    Column(modifier = Modifier.padding(32.dp)) {
        PasswordStrengthIndicator(password = "abc")
//        PasswordStrengthIndicator(password = "abcD123")
//        PasswordStrengthIndicator(password = "abcD123!")
    }
}

@Composable
fun PasswordStrengthIndicator(
    password: String
) {
    val strongColor = MaterialTheme.colorScheme.primary
    val weakColor = MaterialTheme.colorScheme.error
    val mediumColor = MaterialTheme.colorScheme.secondary

    // Memoize expensive password validation calculations
    val passwordStrength = remember(password) {
        PasswordStrength.calculate(password)
    }

    val progressColor = when(passwordStrength.score) {
        0, 1 -> weakColor
        2, 3 -> mediumColor
        else -> strongColor
    }

    val animatedProgress by animateFloatAsState(
        targetValue = passwordStrength.score / 4f,
        label = "password strength"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Only show the first unmet criterion to avoid UI clutter
        passwordStrength.firstUnmetCriterion?.let { criterion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = criterion,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = progressColor,
            trackColor = ProgressIndicatorDefaults.linearTrackColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

// Separate data class for better performance and testability
private data class PasswordStrength(
    val score: Int,
    val firstUnmetCriterion: String?
) {
    companion object {
        fun calculate(password: String): PasswordStrength {
            val criteria = listOf(
                "At least 8 characters" to (password.length >= 8),
                "Contains uppercase letter" to password.any { it.isUpperCase() },
                "Contains lowercase letter" to password.any { it.isLowerCase() },
                "Contains number" to password.any { it.isDigit() },
            )

            val score = criteria.count { it.second }
            val firstUnmetCriterion = criteria.firstOrNull { !it.second }?.first

            return PasswordStrength(score, firstUnmetCriterion)
        }
    }
}