package com.skinsense.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val authRepository: com.skinsense.ai.data.AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount = _totalUnreadCount.asStateFlow()

    init {
        observeUnreadCounts()
    }

    private fun observeUnreadCounts() {
        viewModelScope.launch {
            authRepository.currentUserId.collect { userId ->
                android.util.Log.d("NotificationVM", "Current user ID: $userId")
                if (userId != null) {
                    chatRepository.getTotalUnreadCount(userId).collect { count ->
                        android.util.Log.d("NotificationVM", "Total unread count: $count")
                        _totalUnreadCount.value = count
                    }
                } else {
                    _totalUnreadCount.value = 0
                }
            }
        }
    }
}

class NotificationViewModelFactory(
    private val authRepository: com.skinsense.ai.data.AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(authRepository, chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
