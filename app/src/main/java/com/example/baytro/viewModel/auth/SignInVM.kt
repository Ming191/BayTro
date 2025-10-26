package com.example.baytro.viewModel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.SignInFormState
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.qr_session.QrSessionRepository
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.example.baytro.data.user.UserRoleCache
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.SignInUiState
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class SignInVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val contractRepository: ContractRepository,
    private val qrSessionRepository: QrSessionRepository,
    private val roleCache: UserRoleCache
) : ViewModel() {
    private val _signInUIState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val signInUIState: StateFlow<SignInUiState> = _signInUIState

    private val _signInFormState = MutableStateFlow(SignInFormState())
    val signInFormState: StateFlow<SignInFormState> = _signInFormState

    fun resetState() {
        Log.d("SignInVM", "resetState() called - Resetting UI state to Idle")
        _signInUIState.value = SignInUiState.Idle
        _signInFormState.value = SignInFormState()
        Log.d("SignInVM", "resetState() completed")
    }

    fun onEmailChange(email: String) {
        _signInFormState.value = _signInFormState.value.copy(email = email, emailError = ValidationResult.Success)
    }

    fun onPasswordChange(password: String) {
        _signInFormState.value = _signInFormState.value.copy(password = password, passwordError = ValidationResult.Success)
    }

    fun login() {
        val formState = _signInFormState.value
        if (validateInput(formState)) {
            performLogin(formState.email, formState.password)
        }
    }

    private fun validateInput(
        formState: SignInFormState
    ): Boolean {
        val emailValidator = Validator.validateEmail(formState.email)
        val passwordValidator = Validator.validatePassword(formState.password)
        val isValid = emailValidator == ValidationResult.Success && passwordValidator == ValidationResult.Success

        _signInFormState.value = formState.copy(
            emailError = emailValidator,
            passwordError = passwordValidator
        )
        return isValid
    }

    private fun performLogin(email: String, password: String) {
        viewModelScope.launch {
            _signInUIState.value = SignInUiState.Loading
            try {
                val user = authRepository.signIn(email, password)

                if (authRepository.checkVerification()) {
                    val repoUser = userRepository.getById(user.uid)
                    if (repoUser == null || repoUser.role == null) {
                        _signInUIState.value = SignInUiState.FirstTimeUser(user)
                    } else {
                        repoUser.role.let { role ->
                            com.example.baytro.data.user.UserRoleState.setRole(role)
                            roleCache.setRoleType(user.uid, role)
                        }

                        if (repoUser.role is Role.Tenant) {
                            val hasPendingSession = qrSessionRepository.hasScannedSession(user.uid)
                            if (hasPendingSession) {
                                _signInUIState.value = SignInUiState.TenantPendingSession(user)
                                return@launch
                            }

                            val isInContract = contractRepository.isUserInAnyContract(user.uid)
                            if (!isInContract) {
                                _signInUIState.value = SignInUiState.TenantNoContract(user)
                                return@launch
                            }

                            _signInUIState.value = SignInUiState.TenantWithContract(user)
                            return@launch
                        }
                        _signInUIState.value = SignInUiState.Success(user)
                    }
                } else {
                    authRepository.sendVerificationEmail()
                    authRepository.signOut()
                    _signInUIState.value = SignInUiState.NeedVerification("Email is not verified. We have sent you a new verification email.")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthException -> when (e.errorCode) {
                        "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Incorrect email or password."
                        "ERROR_USER_DISABLED" -> "Your account has been disabled."
                        else -> "Sign in failed. Please try again."
                    }
                    is UnknownHostException -> "No network connection."
                    else -> e.message
                }
                _signInUIState.value = SignInUiState.Error(errorMessage ?: "An unknown error occurred")
            }
        }
    }
}