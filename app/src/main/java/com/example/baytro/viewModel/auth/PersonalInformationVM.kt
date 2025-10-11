package com.example.baytro.viewModel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PersonalInformationVM (
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadPersonalInformation () {
        viewModelScope.launch {
            Log.d("PersonalInformationVM", "loadPersonalInformation() started - Setting isLoading = true")
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                Log.d("PersonalInformationVM", "Fetching User Information: ${currentUser.uid}")

                _user.value = userRepository.getById(currentUser.uid)
            } catch (e: Exception) {
                Log.d("PersonalInformationVM", "error loadPersonalInformation()", e)
            } finally {
                Log.d("PersonalInformationVM", "loadPersonalInformation() finished - Setting isLoading = false, hasLoadedOnce = true")
                _isLoading.value = false
            }
        }
    }
}