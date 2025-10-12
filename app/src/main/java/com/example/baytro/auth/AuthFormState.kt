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

    val confirmNewPassword: String = "",
    val confirmNewPasswordError: ValidationResult = ValidationResult.Success
)

sealed class RoleFormState {
    data class Tenant(val form: EditTenantInformationFormState) : RoleFormState()
    data class Landlord(val form: EditLandlordInformationFormState) : RoleFormState()
}

data class EditTenantInformationFormState (
    val occupation: String = "",
    val idCardNumber: String = "",
    val idCardImageFrontUrl: String? = null,
    val idCardImageBackUrl: String? = null,
    val idCardIssueDate: String = "",
    val emergencyContact: String = ""
)

data class EditLandlordInformationFormState (
    val bankCode: String = "",
    val bankAccountNumber: String = ""
)

data class EditPersonalInformationFormState(
    val fullName: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val gender: Gender? = null,
    val phoneNumber: String = "",
    val email: String = "",
    val imgProfile: String = "",
    val role: Role? = null,

    val dateOfBirthError: ValidationResult = ValidationResult.Success,
    val phoneNumberError: ValidationResult = ValidationResult.Success
)