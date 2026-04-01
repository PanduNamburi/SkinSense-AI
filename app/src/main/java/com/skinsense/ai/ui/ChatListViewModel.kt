package com.skinsense.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.AuthRepository
import com.skinsense.ai.data.Chat
import com.skinsense.ai.data.ChatRepository
import com.skinsense.ai.data.UserRepository
import com.skinsense.ai.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    init {
        fetchChats()
    }

    private fun fetchChats() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId.value ?: return@launch
            // Fetch role from user profile since AuthRepository doesn't store it
            val userProfileResult = userRepository.getUserProfile(userId)
            val role = userProfileResult.getOrNull()?.role ?: UserRole.PATIENT

            if (role == UserRole.DOCTOR) {
                chatRepository.getDoctorChats(userId).collect { chatList ->
                    _chats.value = chatList
                }
            } else {
                chatRepository.getPatientChats(userId).collect { chatList ->
                    _chats.value = chatList
                }
            }
        }
    }
}

class ChatListViewModelFactory(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatListViewModel(authRepository, chatRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
