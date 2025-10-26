package com.example.baytro.utils

import com.google.firebase.functions.FirebaseFunctionsException
import dev.gitlive.firebase.firestore.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
        } catch (_: Exception) {
            amount
        }
    }

    fun formatCompactCurrency(amount: Double): String {
        return try {
            when {
                amount >= 1_000_000_000 -> {
                    val billions = amount / 1_000_000_000
                    String.format(Locale.US, "%.1fB", billions)
                }
                amount >= 1_000_000 -> {
                    val millions = amount / 1_000_000
                    String.format(Locale.US, "%.1fM", millions)
                }
                amount >= 1_000 -> {
                    val thousands = amount / 1_000
                    String.format(Locale.US, "%.1fK", thousands)
                }
                else -> {
                    String.format(Locale.US, "%.0f", amount)
                }
            }
        } catch (_: Exception) {
            amount.toString()
        }
    }

    fun formatVND(amount: String): String {
        return try {
            val numericAmount = amount.replace("[^\\d]".toRegex(), "").toLongOrNull() ?: 0L
            if (numericAmount == 0L) return ""
            val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
            "${formatter.format(numericAmount)} VND"
        } catch (_: Exception) {
            amount
        }
    }

    fun parseVND(formattedAmount: String): String {
        return formattedAmount.replace("[^\\d]".toRegex(), "")
    }

    fun getMonthName(month: Int, short: Boolean = false): String {
        // Calendar.MONTH is 0-based, so we subtract 1
        if (month < 1 || month > 12) return "Invalid Month"

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, month - 1)

        val format = if (short) "MMM" else "MMMM"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(calendar.time)
    }

    fun formatTimestamp(timestamp: Timestamp, pattern: String = "dd MMMM, yyyy"): String {
        return try {
            val date = Date(timestamp.seconds * 1000)
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            formatter.format(date)
        } catch (_: Exception) {
            ""
        }
    }

    fun parseDateToDate(dateStr: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.parse(dateStr) ?: Date()
        } catch (_: Exception) {
            Date()
        }
    }

    /**
     * Parses FirebaseFunctionsException to extract user-friendly error messages.
     *
     * @param e The FirebaseFunctionsException to parse
     * @return A user-friendly error message
     */
    fun parseFirebaseError(e: FirebaseFunctionsException): String {
        return when (e.code) {
            FirebaseFunctionsException.Code.OK -> "Operation completed successfully"
            FirebaseFunctionsException.Code.CANCELLED -> "Operation was cancelled"
            FirebaseFunctionsException.Code.UNKNOWN -> "An unknown error occurred"
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> e.message ?: "Invalid argument provided"
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Request timeout. Please try again"
            FirebaseFunctionsException.Code.NOT_FOUND -> e.message ?: "Resource not found"
            FirebaseFunctionsException.Code.ALREADY_EXISTS -> e.message ?: "Resource already exists"
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Permission denied. You don't have access to this resource"
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> "Resource exhausted. Please try again later"
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> e.message ?: "Operation failed precondition check"
            FirebaseFunctionsException.Code.ABORTED -> "Operation was aborted. Please try again"
            FirebaseFunctionsException.Code.OUT_OF_RANGE -> e.message ?: "Value out of range"
            FirebaseFunctionsException.Code.UNIMPLEMENTED -> "This operation is not implemented"
            FirebaseFunctionsException.Code.INTERNAL -> "Internal server error. Please try again later"
            FirebaseFunctionsException.Code.UNAVAILABLE -> "Service is currently unavailable. Please try again later"
            FirebaseFunctionsException.Code.DATA_LOSS -> "Data loss occurred. Please contact support"
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "You must be signed in to perform this action"
        }
    }


}