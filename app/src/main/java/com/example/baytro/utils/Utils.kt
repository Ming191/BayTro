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

    fun formatVND(amount: String): String {
        return try {
            val numericAmount = amount.replace("[^\\d]".toRegex(), "").toLongOrNull() ?: 0L
            if (numericAmount == 0L) return ""
            val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
            "${formatter.format(numericAmount)} VND"
        } catch (e: Exception) {
            amount
        }
    }

    fun parseVND(formattedAmount: String): String {
        return formattedAmount.replace("[^\\d]".toRegex(), "")
    }
}