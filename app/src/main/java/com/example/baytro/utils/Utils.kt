package com.example.baytro.utils

import androidx.compose.runtime.Composable
import java.text.NumberFormat
import java.util.Locale

object Utils {
    fun formatOrdinal(day: Int): String {
        val suffix = if (day in 11..13) {
            "th"
        } else {
            when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
        return "$day$suffix"
    }

    fun formatCurrency(amount: String): String {
        return try {
            val numericAmount = amount.toDoubleOrNull() ?: 0.0
            val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
            formatter.format(numericAmount)
        } catch (e: Exception) {
            amount
        }
    }
}