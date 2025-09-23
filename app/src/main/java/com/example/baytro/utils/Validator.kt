package com.example.baytro.utils

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

class Validator {
    private val ERROR_EMAIL_EMPTY = "Vui lòng nhập email."
    private val ERROR_EMAIL_INVALID = "Email không hợp lệ."
    private val ERROR_PASSWORD_EMPTY = "Vui lòng nhập mật khẩu."
    private val ERROR_PASSWORD_MISMATCH = "Mật khẩu xác nhận không khớp."
    private val ERROR_PASSWORD_LENGTH = "Mật khẩu phải có ít nhất 8 ký tự."
    private val ERROR_PASSWORD_UPPERCASE = "Mật khẩu phải chứa ít nhất một chữ hoa."
    private val ERROR_PASSWORD_LOWERCASE = "Mật khẩu phải chứa ít nhất một chữ thường."
    private val ERROR_PASSWORD_DIGIT = "Mật khẩu phải chứa ít nhất một chữ số."

    private val ERROR_CONFIRM_PASSWORD_EMPTY = "Vui lòng nhập mật khẩu xác nhận."

    fun validateEmail(email: String): ValidationResult = when {
        email.isBlank() -> ValidationResult.Error(ERROR_EMAIL_EMPTY)
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult.Error(ERROR_EMAIL_INVALID)
        else -> ValidationResult.Success
    }

    fun validatePassword(password: String): ValidationResult =
        if (password.isBlank()) ValidationResult.Error(ERROR_PASSWORD_EMPTY) else ValidationResult.Success

    fun validatePasswordStrength(password: String): ValidationResult {
        if (password.length < 8) {
            return ValidationResult.Error(ERROR_PASSWORD_LENGTH)
        }
        if (!password.any { it.isUpperCase() }) {
            return ValidationResult.Error(ERROR_PASSWORD_UPPERCASE)
        }
        if (!password.any { it.isLowerCase() }) {
            return ValidationResult.Error(ERROR_PASSWORD_LOWERCASE)
        }
        if (!password.any { it.isDigit() }) {
            return ValidationResult.Error(ERROR_PASSWORD_DIGIT)
        }
        return ValidationResult.Success
    }
    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult =
        if (password != confirmPassword) ValidationResult.Error(ERROR_PASSWORD_MISMATCH)
        else if (confirmPassword.isBlank()) ValidationResult.Error(ERROR_CONFIRM_PASSWORD_EMPTY)
        else ValidationResult.Success
}