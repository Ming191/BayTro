package com.example.baytro.viewModel.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import com.example.baytro.data.user.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

class PersonalInformationVM (
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository,
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
    private val contractRepository: ContractRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    sealed class ProfileHeaderState {
        data class Landlord(
            val buildings: Int,
            val rooms: Int,
            val tenants: Int
        ) : ProfileHeaderState()

        data class Tenant(
            val buildingName: String?,
            val roomName: String?
        ) : ProfileHeaderState()
    }

    private val _headerState = MutableStateFlow<ProfileHeaderState?>(null)
    val headerState: StateFlow<ProfileHeaderState?> = _headerState

    fun loadPersonalInformation () {
        viewModelScope.launch {
            Log.d("PersonalInformationVM", "loadPersonalInformation() started - Setting isLoading = true")
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                Log.d("PersonalInformationVM", "Fetching User Information: ${currentUser.uid}")

                val dbUser = userRepository.getById(currentUser.uid)
                _user.value = dbUser

                when (dbUser?.role) {
                    is Role.Landlord -> {
                        val buildingsDeferred = async { runCatching { buildingRepository.getBuildingsByUserId(currentUser.uid) }.getOrDefault(emptyList()) }
                        val contractsDeferred = async { runCatching { contractRepository.getContractsByLandlordId(currentUser.uid) }.getOrDefault(emptyList()) }
                        
                        val userBuildings = buildingsDeferred.await()
                        val contracts = contractsDeferred.await()
                        
                        val buildings = userBuildings.size
                        val rooms = if (userBuildings.isNotEmpty()) {
                            val roomsDeferred = userBuildings.map { building ->
                                async { runCatching { roomRepository.getRoomsByBuildingId(building.id).size }.getOrDefault(0) }
                            }
                            roomsDeferred.sumOf { it.await() }
                        } else 0
                        val tenants = contracts.flatMap { it.tenantIds }.distinct().size
                        
                        _headerState.value = ProfileHeaderState.Landlord(
                            buildings = buildings,
                            rooms = rooms,
                            tenants = tenants
                        )
                    }
                    is Role.Tenant -> {
                        val activeDeferred = async { runCatching { contractRepository.getActiveContract(currentUser.uid) }.getOrNull() }
                        val active = activeDeferred.await()
                        
                        val buildingNameDeferred = active?.buildingId?.let { buildingId ->
                            async { runCatching { buildingRepository.getById(buildingId)?.name }.getOrNull() }
                        }
                        val roomNameDeferred = active?.roomId?.let { roomId ->
                            async { runCatching { roomRepository.getById(roomId)?.roomNumber }.getOrNull() }
                        }
                        
                        val buildingName = buildingNameDeferred?.await()
                        val roomName = roomNameDeferred?.await()
                        
                        _headerState.value = ProfileHeaderState.Tenant(
                            buildingName = buildingName,
                            roomName = roomName
                        )
                    }
                    else -> {
                        _headerState.value = null
                    }
                }
            } catch (e: Exception) {
                Log.d("PersonalInformationVM", "error loadPersonalInformation()", e)
            } finally {
                Log.d("PersonalInformationVM", "loadPersonalInformation() finished - Setting isLoading = false, hasLoadedOnce = true")
                _isLoading.value = false
            }
        }
    }

    fun updateProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            try {
                val uid = authRepository.getCurrentUser()?.uid ?: return@launch
                val downloadUrl = mediaRepository.uploadUserImage(uid, imageUri, "profile", "avatar")
                userRepository.updateUserProfileImageUrl(uid, downloadUrl)
                _user.value = userRepository.getById(uid)
            } catch (e: Exception) {
                Log.e("PersonalInformationVM", "updateProfileImage error", e)
            }
        }
    }

}