package com.example.baytro.viewModel.splash

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.BankCode
import com.example.baytro.data.Gender
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.Role
import com.example.baytro.data.User
import com.example.baytro.data.UserRepository
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewLandlordUserVM(
    private val context: Context,
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

    fun onPhoneNumberChange(phoneNumber: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(phoneNumber = phoneNumber, phoneNumberError = ValidationResult.Success)
    }

    private fun validateInput(): Boolean {
        val formState = _newLandlordUserFormState.value
        val fullNameValidator = Validator.validateNonEmpty(formState.fullName, "Full Name")
        val permanentAddressValidator = Validator.validateNonEmpty(formState.permanentAddress, "Permanent Address")
        val dateOfBirthValidator = Validator.validateNonEmpty(formState.dateOfBirth, "Date of Birth")
        val bankAccountNumberValidator = Validator.validateNonEmpty(formState.bankAccountNumber , "Bank Account Number")
        val phoneNumberValidator = Validator.validatePhoneNumber(formState.phoneNumber)
        val avatarUriValidator = if (formState.avatarUri == Uri.EMPTY) ValidationResult.Error("Please select an avatar") else ValidationResult.Success
        val isValid = fullNameValidator == ValidationResult.Success &&
                permanentAddressValidator == ValidationResult.Success &&
                dateOfBirthValidator == ValidationResult.Success &&
                bankAccountNumberValidator == ValidationResult.Success &&
                phoneNumberValidator == ValidationResult.Success &&
                avatarUriValidator == ValidationResult.Success



        _newLandlordUserFormState.value = formState.copy(
            fullNameError = fullNameValidator,
            permanentAddressError = permanentAddressValidator,
            dateOfBirthError = dateOfBirthValidator,
            bankAccountNumberError = bankAccountNumberValidator,
            phoneNumberError = phoneNumberValidator,
            avatarUriError = avatarUriValidator
            )
        return isValid
    }

    fun submit() {
        onSubmit()
    }

    /**
     * Handles the submission of the new landlord user form.
     * It validates the input fields, and if valid, proceeds to:
     * 1. Get the current authenticated user.
     * 2. Compress the selected avatar image.
     * 3. Upload the compressed image to get a profile image URL.
     * 4. Create a [User] object with the landlord role and form data.
     * 5. Save the new user to the repository.
     *
     * The UI state is updated to [UiState.Loading] during the process,
     * then to [UiState.Success] with the created [User] on success,
     * or [UiState.Error] if any step fails or validation is unsuccessful.
     */
    private fun onSubmit() {
        _newLandlordUserUIState.value = UiState.Loading
        val formState = _newLandlordUserFormState.value
        Log.d("NewLandlordUserVM", "Submit called with formState: $formState")

        if (!validateInput()) {
            Log.w("NewLandlordUserVM", "Validation failed")
            _newLandlordUserUIState.value = UiState.Error("Please fix the errors in the form.")
            return
        }

        viewModelScope.launch {
            _newLandlordUserUIState.value = UiState.Loading
            try {
                Log.d("NewLandlordUserVM", "Fetching auth user...")
                val authUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("User is not authenticated. Please log in again.")
                Log.d("NewLandlordUserVM", "Auth user found: ${authUser.uid}")

                Log.d("NewLandlordUserVM", "Compressing image: ${formState.avatarUri}")
                val compressedFile = ImageProcessor.compressImageWithCoil(
                    context = context,
                    uri = formState.avatarUri
                )
                val compressedFileUri = Uri.fromFile(compressedFile)
                Log.d("NewLandlordUserVM", "Compressed image saved at: $compressedFileUri")

                Log.d("NewLandlordUserVM", "Uploading profile image...")
                val profileImgUrl = mediaRepository.uploadUserProfileImage(
                    userId = authUser.uid,
                    imageUri = compressedFileUri
                )
                Log.d("NewLandlordUserVM", "Profile image uploaded: $profileImgUrl")

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
                Log.d("NewLandlordUserVM", "Saving user: $user")

                userRepository.addWithId(user.id, user)
                Log.d("NewLandlordUserVM", "User saved successfully")

                _newLandlordUserUIState.value = UiState.Success(user)
            } catch (e: Exception) {
                Log.e("NewLandlordUserVM", "Submit failed", e)
                _newLandlordUserUIState.value =
                    UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}