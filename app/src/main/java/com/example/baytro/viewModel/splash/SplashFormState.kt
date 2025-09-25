package com.example.baytro.viewModel.splash

import android.net.Uri
import com.example.baytro.data.BankCode
import com.example.baytro.data.Gender
import com.example.baytro.data.RoleType
import com.example.baytro.utils.ValidationResult
data class splashFormState(
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