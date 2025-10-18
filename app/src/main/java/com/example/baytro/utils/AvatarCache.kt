package com.example.baytro.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.baytro.data.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AvatarCache(private val userRepository: UserRepository) {
    private var _cachedAvatarUrl by mutableStateOf<String?>(null)
    private var _currentUserId by mutableStateOf<String?>(null)
    private var _isLoading by mutableStateOf(false)
    
    val avatarUrl: String? get() = _cachedAvatarUrl
    val isLoading: Boolean get() = _isLoading
    
    fun loadAvatar(userId: String) {
        if (_currentUserId == userId && _cachedAvatarUrl != null) {
            return // Already cached for this user
        }
        
        if (_isLoading) return // Already loading
        
        _isLoading = true
        _currentUserId = userId
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = userRepository.getById(userId)
                _cachedAvatarUrl = user?.profileImgUrl
            } catch (e: Exception) {
                // Keep existing cached value on error
            } finally {
                _isLoading = false
            }
        }
    }
    
    fun clearCache() {
        _cachedAvatarUrl = null
        _currentUserId = null
        _isLoading = false
    }
    
    fun updateAvatar(newUrl: String?) {
        _cachedAvatarUrl = newUrl
    }
}

val LocalAvatarCache = staticCompositionLocalOf<AvatarCache> { 
    error("AvatarCache not provided") 
}
