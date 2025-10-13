package com.example.baytro.auth

import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.Role
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
    val newPasswordStrengthError:  ValidationResult = ValidationResult.Success,

    val confirmNewPassword: String = "",
    val confirmNewPasswordError: ValidationResult = ValidationResult.Success
)

sealed class RoleFormState {
    data class Tenant(
        val idCardNumber: String = "",
        val idCardImageFrontUrl: String? = null,
        val idCardImageBackUrl: String? = null,
        val idCardIssueDate: String = "",
    ) : RoleFormState()
    data class Landlord(
        val bankCode: String = "",
        val bankAccountNumber: String = ""
    ) : RoleFormState()
}

data class EditPersonalInformationFormState(
    val fullName: String = "",
    val fullNumberError: ValidationResult = ValidationResult.Success,

    val dateOfBirth: String = "",
    val dateOfBirthError: ValidationResult = ValidationResult.Success,

    val address: String = "",
    val addressError: ValidationResult = ValidationResult.Success,

    val gender: Gender? = null,
    val genderError: ValidationResult = ValidationResult.Success,

    val phoneNumber: String = "",
    val phoneNumberError: ValidationResult = ValidationResult.Success,

    val email: String = "",
    val role: Role? = null,
    val profileImgUrl: String? = null
)