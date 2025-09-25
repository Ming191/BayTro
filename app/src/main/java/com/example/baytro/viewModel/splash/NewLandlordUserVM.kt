package com.example.baytro.viewModel.splash

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.BankCode
import com.example.baytro.data.Gender
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.Role
import com.example.baytro.data.RoleType
import com.example.baytro.data.User
import com.example.baytro.data.UserRepository
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewLandlordUserVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val _newLandlordUserFormState = MutableStateFlow(NewLandlordUserFormState())
    val newLandlordUserFormState: StateFlow<NewLandlordUserFormState> = _newLandlordUserFormState

    private val _newLandlordUserUIState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val newLandlordUserUIState: StateFlow<UiState<User>> = _newLandlordUserUIState

    fun onFullNameChange(fullName: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(fullName = fullName, fullNameError = ValidationResult.Success)
    }

    fun onPermanentAddressChange(permanentAddress: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(permanentAddress = permanentAddress, permanentAddressError = ValidationResult.Success)
    }

    fun onDateOfBirthChange(dateOfBirth: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(dateOfBirth = dateOfBirth, dateOfBirthError = ValidationResult.Success)
    }

    fun onBankAccountNumberChange(bankAccountNumber: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(bankAccountNumber = bankAccountNumber, bankAccountNumberError = ValidationResult.Success)
    }

    fun onGenderChange(gender: Gender) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(gender = gender)
    }

    fun onBankCodeChange(bankCode: BankCode) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(bankCode = bankCode)
    }

    fun onAvatarUriChange(avatarUri: Uri) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(avatarUri = avatarUri, avatarUriError = ValidationResult.Success)
    }

    private val _showPhotoSelector = MutableStateFlow(false)
    val showPhotoSelector: StateFlow<Boolean> = _showPhotoSelector
    fun onAvatarClick() {
        _showPhotoSelector.value = true
    }
    fun onPhotoSelectorDismissed() {
        _showPhotoSelector.value = false
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(phoneNumber = phoneNumber, phoneNumberError = ValidationResult.Success)
    }

    private fun validateInput(
        formState: NewLandlordUserFormState
    ): Boolean {
        val fullNameValidator = Validator.validateNonEmpty(formState.fullName, "Full Name")
        val permanentAddressValidator = Validator.validateNonEmpty(formState.permanentAddress, "Permanent Address")
        val dateOfBirthValidator = Validator.validateNonEmpty(formState.dateOfBirth, "Date of Birth")
        val bankAccountNumberValidator = Validator.validateNonEmpty(formState.bankAccountNumber , "Bank Account Number")
        val phoneNumberValidator = Validator.validatePhoneNumber(formState.phoneNumber)
        val isValid = fullNameValidator == ValidationResult.Success &&
                permanentAddressValidator == ValidationResult.Success &&
                dateOfBirthValidator == ValidationResult.Success &&
                bankAccountNumberValidator == ValidationResult.Success &&
                formState.avatarUri != Uri.EMPTY &&
                phoneNumberValidator == ValidationResult.Success


        _newLandlordUserFormState.value = formState.copy(
            fullNameError = fullNameValidator,
            permanentAddressError = permanentAddressValidator,
            dateOfBirthError = dateOfBirthValidator,
            bankAccountNumberError = bankAccountNumberValidator
        )
        return isValid
    }

    fun submit() {
        onSubmit()
    }

    private fun onSubmit() {
        _newLandlordUserUIState.value = UiState.Loading
        val formState = _newLandlordUserFormState.value
        if (!validateInput(formState)) {
            return
        }
        viewModelScope.launch {
            try {
                val authUser = authRepository.getCurrentUser()!!
                val profileImgUrl = mediaRepository.uploadUserProfileImage(
                    userId = authUser.uid,
                    imageUri = formState.avatarUri
                )
                val landlordRole = Role.Landlord(
                    bankCode = formState.bankCode.name,
                    bankAccountNumber = formState.bankAccountNumber,
                )

                val user = User(
                    id = authUser.uid,
                    email = authUser.email!!,
                    fullName = formState.fullName,
                    dateOfBirth = formState.dateOfBirth,
                    gender = formState.gender,
                    address = formState.permanentAddress,
                    phoneNumber = formState.phoneNumber,
                    profileImgUrl = profileImgUrl,
                    role = landlordRole
                )
                userRepository.addWithId(user.id, user)
                _newLandlordUserUIState.value = UiState.Success(user)
            } catch (e: Exception) {
                _newLandlordUserUIState.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}