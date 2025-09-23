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
//    val defaultColor = MaterialTheme.colorScheme.onSurface


    val criteria = listOf(
        "Ít nhất 8 ký tự" to (password.length >= 8),
        "Chứa chữ hoa" to password.any { it.isUpperCase() },
        "Chứa chữ thường" to password.any { it.isLowerCase() },
        "Chứa số" to password.any { it.isDigit() },
    )

    val strength = criteria.count { it.second }
    val progressColor = when(strength) {
        0,1 -> weakColor
        2,4 -> mediumColor
        else -> strongColor
    }
    val animatedProgress by animateFloatAsState(targetValue = strength / 5f, label = "password strength")

    Column(modifier = Modifier.fillMaxWidth()) {
        for ((text, valid) in criteria) {
            if (valid) continue

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            break
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

        Text(
            text = when(strength) {
                in 0..1 -> "Weak"
                in 2..3 -> "Medium"
                4,5 -> "Strong"
                else -> ""
            },
            color = progressColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }
}