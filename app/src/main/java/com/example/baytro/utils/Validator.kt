package com.example.baytro.utils

import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    val isSuccess: Boolean
        get() = this is Success
}

object Validator {
    private const val ERROR_EMAIL_EMPTY = "Please enter your email."
    private const val ERROR_EMAIL_INVALID = "Invalid email address."
    private const val ERROR_PASSWORD_EMPTY = "Please enter your password."
    private const val ERROR_PASSWORD_MISMATCH = "Password confirmation does not match."
    private const val ERROR_PASSWORD_LENGTH = "Password must be at least 8 characters."
    private const val ERROR_PASSWORD_UPPERCASE = "Password must contain at least one uppercase letter."
    private const val ERROR_PASSWORD_LOWERCASE = "Password must contain at least one lowercase letter."
    private const val ERROR_PASSWORD_DIGIT = "Password must contain at least one digit."
    private const val ERROR_CONFIRM_PASSWORD_EMPTY = "Please enter the confirmation password."
    private const val ERROR_PHONE_EMPTY = "Please enter your phone number."
    private const val ERROR_PHONE_INVALID = "Invalid phone number."

    fun validateEmail(email: String): ValidationResult = when {
        email.isBlank() -> ValidationResult.Error(ERROR_EMAIL_EMPTY)
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult.Error(ERROR_EMAIL_INVALID)
        else -> ValidationResult.Success
    }

    fun validatePassword(password: String): ValidationResult =
        if (password.isBlank()) ValidationResult.Error(ERROR_PASSWORD_EMPTY)
        else ValidationResult.Success

    fun validatePasswordStrength(password: String): ValidationResult = when {
        password.length < 8 -> ValidationResult.Error(ERROR_PASSWORD_LENGTH)
        !password.any { it.isUpperCase() } -> ValidationResult.Error(ERROR_PASSWORD_UPPERCASE)
        !password.any { it.isLowerCase() } -> ValidationResult.Error(ERROR_PASSWORD_LOWERCASE)
        !password.any { it.isDigit() } -> ValidationResult.Error(ERROR_PASSWORD_DIGIT)
        else -> ValidationResult.Success
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult = when {
        confirmPassword.isBlank() -> ValidationResult.Error(ERROR_CONFIRM_PASSWORD_EMPTY)
        password != confirmPassword -> ValidationResult.Error(ERROR_PASSWORD_MISMATCH)
        else -> ValidationResult.Success
    }

    fun validatePhoneNumber(phoneNumber: String): ValidationResult {
        val vietnamPhoneRegex = Regex("^(0[3|5|7|8|9][0-9]{8}|\\+84[3|5|7|8|9][0-9]{8})\$")

        return when {
            phoneNumber.isBlank() -> ValidationResult.Error(ERROR_PHONE_EMPTY)
            !vietnamPhoneRegex.matches(phoneNumber) -> ValidationResult.Error(ERROR_PHONE_INVALID)
            else -> ValidationResult.Success
        }
    }

    fun validateNonEmpty(field: String, fieldName: String): ValidationResult =
        if (field.isBlank()) ValidationResult.Error("Please enter your $fieldName.")
        else ValidationResult.Success

    fun validateInteger(field: String, fieldName: String) : ValidationResult =
        if (field.isBlank()) ValidationResult.Error("Please enter your $fieldName.")
        else {
            val intValue = field.toIntOrNull()
            if (intValue == null) {
                ValidationResult.Error("$fieldName must be a valid integer.")
            } else if (intValue < 0) {
                ValidationResult.Error("$fieldName must be a non-negative integer.")
            } else {
                ValidationResult.Success
            }
        }

    fun validatePhotosURL(photosURL: List<String>, maxPhoto: Int = 5): ValidationResult =
        when {
            photosURL.isEmpty() -> ValidationResult.Error("Please add at least one photo.")
            photosURL.size > maxPhoto -> ValidationResult.Error("You can add up to $maxPhoto photos only.")
            else -> ValidationResult.Success
        }

    fun validateStartEndDate(startDate: String, endDate: String): ValidationResult {
        return try {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            if (start.isBefore(end)) ValidationResult.Success
            else ValidationResult.Error("Start date must be before end date.")
        } catch (e: DateTimeParseException) {
            ValidationResult.Error("Invalid date format.")
        }
    }
}