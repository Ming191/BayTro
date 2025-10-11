package com.example.baytro.auth

import com.example.baytro.utils.ValidationResult

data class SignInFormState(
    val email: String = "",
    val emailError: ValidationResult = ValidationResult.Success,

    val password: String = "",
    val passwordError: ValidationResult = ValidationResult.Success,
)

data class SignUpFormState(
    val email: String = "",
    val emailError: ValidationResult = ValidationResult.Success,

    val password: String = "",
    val passwordError: ValidationResult = ValidationResult.Success,
    val passwordStrengthError:  ValidationResult = ValidationResult.Success,

    val confirmPassword: String = "",
    val confirmPasswordError: ValidationResult = ValidationResult.Success,
)

data class ForgotPasswordFormState(
    val email: String = "",
    val emailError: ValidationResult = ValidationResult.Success
)

data class ChangePasswordFormState(
    val password: String = "",
    val passwordError: ValidationResult = ValidationResult.Success,

    val newPassword: String = "",
    val newPasswordError: ValidationResult = ValidationResult.Success,

    val confirmNewPassword: String = "",
    val confirmNewPasswordError: ValidationResult = ValidationResult.Success,
)
