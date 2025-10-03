package com.example.baytro.viewModel.splash

import android.net.Uri
import com.example.baytro.data.BankCode
import com.example.baytro.data.Gender
import com.example.baytro.data.RoleType
import com.example.baytro.utils.ValidationResult
data class SplashFormState(
    val role: RoleType = RoleType.entries[0],
)

data class NewLandlordUserFormState(
    val fullName: String = "",
    val permanentAddress: String = "",
    val dateOfBirth: String = "",
    val bankAccountNumber: String = "",
    val gender: Gender = Gender.entries[0],
    val bankCode: BankCode= BankCode.entries[0],
    val avatarUri: Uri = Uri.EMPTY,

    val fullNameError: ValidationResult = ValidationResult.Success,
    val permanentAddressError: ValidationResult = ValidationResult.Success,
    val dateOfBirthError: ValidationResult = ValidationResult.Success,
    val bankAccountNumberError: ValidationResult = ValidationResult.Success,
    val avatarUriError: ValidationResult = ValidationResult.Success,

    val phoneNumber: String = "",
    val phoneNumberError: ValidationResult = ValidationResult.Success,

)

data class NewTenantUserFormState(
    val fullName: String = "",
    val permanentAddress: String = "",
    val dateOfBirth: String = "",
    val gender: Gender = Gender.entries[0],
    val avatarUri: Uri = Uri.EMPTY,
    val phoneNumber: String = "",

    // Tenant-specific fields
    val occupation: String = "",
    val idCardNumber: String = "",
    val idCardIssueDate: String = "",
    val emergencyContact: String = "",

    // Validation errors
    val fullNameError: ValidationResult = ValidationResult.Success,
    val permanentAddressError: ValidationResult = ValidationResult.Success,
    val dateOfBirthError: ValidationResult = ValidationResult.Success,
    val phoneNumberError: ValidationResult = ValidationResult.Success,
    val avatarUriError: ValidationResult = ValidationResult.Success,
    val occupationError: ValidationResult = ValidationResult.Success,
    val idCardNumberError: ValidationResult = ValidationResult.Success,
    val idCardIssueDateError: ValidationResult = ValidationResult.Success,
    val emergencyContactError: ValidationResult = ValidationResult.Success,
)
