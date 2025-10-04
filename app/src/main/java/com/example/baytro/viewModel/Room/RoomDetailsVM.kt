package com.example.baytro.viewModel.Room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.Building
import com.example.baytro.data.Room
import com.example.baytro.data.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoomDetailsVM(
    private val roomRepository: RoomRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadRoom() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val room = roomRepository.getById(roomId)
                _room.value = room
            } catch (e: Exception) {
                e.printStackTrace()
                _room.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}