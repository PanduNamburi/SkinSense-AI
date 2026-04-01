package com.skinsense.ai.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.GroqChatRepository
import com.skinsense.ai.data.OllamaChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatbotViewModel : ViewModel() {

    private val repository = GroqChatRepository()

    // Conversation history shown in the UI (role: "user" or "assistant", no system messages)
    val messages = mutableStateListOf<OllamaChatMessage>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        val userMessage = OllamaChatMessage(role = "user", content = userText.trim())
        messages.add(userMessage)

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.chat(messages.toList())
            result.onSuccess { reply ->
                messages.add(OllamaChatMessage(role = "assistant", content = reply))
            }.onFailure { error ->
                _errorMessage.value = error.message
                // Remove the user message if sending failed so they can retry
                messages.removeLastOrNull()
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearChat() {
        messages.clear()
    }
}
