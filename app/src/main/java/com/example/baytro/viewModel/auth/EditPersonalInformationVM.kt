package com.example.baytro.viewModel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.EditPersonalInformationFormState
import com.example.baytro.auth.RoleFormState
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditPersonalInformationVM (
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _editPersonalInformationUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val editPersonalInformationUIState: StateFlow<AuthUIState> = _editPersonalInformationUIState

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user
    private val _editPersonalInformationFormState = MutableStateFlow(EditPersonalInformationFormState())
    val editPersonalInformationFormState: StateFlow<EditPersonalInformationFormState> = _editPersonalInformationFormState

    private val _editRoleInformationFormState = MutableStateFlow<RoleFormState?>(null)
    val editRoleInformationFormState: StateFlow<RoleFormState?> = _editRoleInformationFormState

    fun loadEditPersonalInformation() {
        viewModelScope.launch {
            _editPersonalInformationUIState.value = AuthUIState.Loading
            val auth = authRepository.getCurrentUser()
            val user = userRepository.getById(auth!!.uid)
            if (user == null) {
                _editPersonalInformationUIState.value = AuthUIState.Error("Fail to get User")
            } else {
                _editPersonalInformationFormState.value = EditPersonalInformationFormState(
                    fullName = user.fullName,
                    dateOfBirth = user.dateOfBirth,
                    address = user.address,
                    gender = user.gender,
                    phoneNumber = user.phoneNumber,
                    email = user.email,
                    role = user.role,
                    profileImgUrl = user.profileImgUrl
                )
                when (user.role) {
                    is Role.Tenant -> {
                        _editRoleInformationFormState.value = RoleFormState.Tenant(
                            idCardImageBackUrl = user.role.idCardImageBackUrl,
                            idCardImageFrontUrl = user.role.idCardImageFrontUrl,
                            idCardNumber = user.role.idCardNumber,
                            idCardIssueDate = user.role.idCardIssueDate
                        )
                    }
                    is Role.Landlord -> {
                        _editRoleInformationFormState.value = RoleFormState.Landlord(
                            bankCode = user.role.bankCode,
                            bankAccountNumber = user.role.bankAccountNumber
                        )
                    }
                    else -> {
                        _editPersonalInformationUIState.value = AuthUIState.Error("Unknown user role")
                    }
                }
            }
            _editPersonalInformationUIState.value = AuthUIState.Success(auth)
        }
    }

    fun onFullNameChange(fullName: String) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            fullName = fullName,
            fullNumberError = ValidationResult.Success
        )
    }

    fun onDateOfBirthChange(dateOfBirth: String) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            dateOfBirth = dateOfBirth,
            dateOfBirthError = ValidationResult.Success
        )
    }

    fun onGenderChange(gender: Gender) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            gender = gender,
            genderError = ValidationResult.Success
        )
    }

    fun onAddressChange(address: String) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            address = address,
            addressError = ValidationResult.Success
        )
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            phoneNumber = phoneNumber,
            phoneNumberError = ValidationResult.Success
        )
    }
    fun onBankCodeChange(bankCode: String) {
        when (val currentRole = _editRoleInformationFormState.value) {
            is RoleFormState.Landlord -> {
                _editRoleInformationFormState.value = currentRole.copy(
                    bankCode = bankCode
                )
            }
            else -> {
                return
            }
        }
    }

    fun onBankAccountNumberChange(bankAccountNumber: String) {
        when (val currentRole = _editRoleInformationFormState.value) {
            is RoleFormState.Landlord -> {
                _editRoleInformationFormState.value = currentRole.copy(
                    bankAccountNumber = bankAccountNumber
                )
            }
            else -> {
                return
            }
        }
    }

    private fun validateInput() : Boolean{
        Log.d("EditPersonalInformationVM", "validateInput: start")
        val formState = _editPersonalInformationFormState.value
        val fullNameValidator = Validator.validateNonEmpty(formState.fullName, "Full name")
        val dateOfBirthValidator = Validator.validateNonEmpty(formState.dateOfBirth, "Date of birth")
        val genderValidator =
            if (formState.gender != Gender.MALE && formState.gender != Gender.FEMALE)
                ValidationResult.Error("Please choose your gender")
            else
                ValidationResult.Success
        val addressValidator = Validator.validateNonEmpty(formState.address, "Address")
        val phoneNumberValidator = Validator.validatePhoneNumber(formState.phoneNumber)
        val bankAccountNumberValidator =
            when (val currentRole = _editRoleInformationFormState.value) {
                is RoleFormState.Landlord -> { Validator.validateNonEmpty(currentRole.bankCode, "Bank code")
                }
                else -> {
                    ValidationResult.Success
                }
            }
        val bankCodeValidator =
            when (val currentRole = _editRoleInformationFormState.value) {
                is RoleFormState.Landlord -> { Validator.validateNonEmpty(currentRole.bankAccountNumber, "Bank account code")
                }
                else -> {
                    ValidationResult.Success
                }
            }

        val allResults = listOf(
            fullNameValidator,
            dateOfBirthValidator,
            genderValidator,
            addressValidator,
            phoneNumberValidator,
            bankAccountNumberValidator,
            bankCodeValidator
        )

        val isValid = allResults.all { it == ValidationResult.Success }

        if (!isValid) {
            val errors = buildList {
                if (fullNameValidator != ValidationResult.Success) add("fullName=$fullNameValidator")
                if (dateOfBirthValidator != ValidationResult.Success) add("dateOfBirth=$dateOfBirthValidator")
                if (genderValidator != ValidationResult.Success) add("gender=$genderValidator")
                if (addressValidator != ValidationResult.Success) add("address=$addressValidator")
                if (phoneNumberValidator != ValidationResult.Success) add("phoneNumber=$phoneNumberValidator")
                if (bankAccountNumberValidator != ValidationResult.Success) add("bankAccountNumber=${bankAccountNumberValidator}Validator")
                if (bankCodeValidator != ValidationResult.Success) add("bankCode=$bankCodeValidator")
            }
            Log.w("EditPersonalInformationVM", "validateInput: validation failed -> ${errors.joinToString(", ")}")
        }

        Log.d("EditPersonalInformationVM", "validateInput: result isValid=$isValid")
        _editPersonalInformationFormState.value = formState.copy(
            fullNumberError = fullNameValidator,
            dateOfBirthError = dateOfBirthValidator,
            genderError = genderValidator,
            addressError = addressValidator,
            phoneNumberError = phoneNumberValidator
        )
        return isValid
    }

    fun onChangePersonalInformationClicked() {
        viewModelScope.launch {
            _editPersonalInformationUIState.value = AuthUIState.Loading
            try {
                if (validateInput()) {
                    val auth = authRepository.getCurrentUser()
                    val user = _editPersonalInformationFormState.value
                    val editedUser = User(
                        id = "",
                        fullName = user.fullName,
                        dateOfBirth = user.dateOfBirth,
                        gender = user.gender,
                        role = user.role,
                        address = user.address,
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                        profileImgUrl = user.profileImgUrl,
                    )
                    if (user.role is Role.Landlord) {
                        val roleUser = _editRoleInformationFormState.value as RoleFormState.Landlord
                        val editedRole = Role.Landlord(
                            bankCode = roleUser.bankCode,
                            bankAccountNumber = roleUser.bankAccountNumber
                        )
                        editedUser.copy(role = editedRole)
                    }
                    userRepository.update(auth!!.uid, editedUser)
                    _editPersonalInformationUIState.value = AuthUIState.Success(auth)
                } else {
                    Log.d("EditPersonalInformationVM", "fail to validate")
                }
            } catch (e: Exception) {
                Log.e("EditPersonalInformationVM", "onSubmit: error update personal information", e)
                _editPersonalInformationUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred while updating personal information")
            }
        }
    }
}