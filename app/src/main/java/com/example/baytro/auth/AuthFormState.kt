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
    val passwordMatchError: ValidationResult = ValidationResult.Success,
)
