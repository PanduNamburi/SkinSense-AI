package com.skinsense.ai.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: com.skinsense.ai.data.UserRepository
) : ViewModel() {
    
    val currentUserId: String? = authRepository.currentUserId.value

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    init {
        fetchMessages()
    }

    private fun fetchMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(text: String, type: ChatMessageType = ChatMessageType.TEXT, attachmentUrl: String? = null, report: ReportAttachment? = null) {
        if (text.isBlank() && attachmentUrl == null && report == null) return

        viewModelScope.launch {
            val senderId = authRepository.currentUserId.value ?: return@launch
            // Fetch role
            val userProfileResult = userRepository.getUserProfile(senderId)
            val role = userProfileResult.getOrNull()?.role ?: UserRole.PATIENT

            val message = ChatMessage(
                chatId = chatId,
                senderId = senderId,
                senderRole = role.name,
                messageText = text,
                messageType = type,
                attachmentUrl = attachmentUrl,
                reportAttachment = report,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.sendMessage(chatId, message, senderId)
        }
    }

    fun sendImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            val uploadResult = chatRepository.uploadImage(context, uri)
            _isUploading.value = false
            
            uploadResult.onSuccess { url ->
                sendMessage(text = "", type = ChatMessageType.IMAGE, attachmentUrl = url)
            }.onFailure { e ->
                _uploadError.value = "Upload failed: ${e.message}"
            }
        }
    }

    fun clearUploadError() { _uploadError.value = null }

    fun sendReport(report: ReportAttachment) {
        sendMessage(text = "", type = ChatMessageType.REPORT, report = report)
    }

    fun markAsRead() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId.value ?: return@launch
            chatRepository.markChatAsRead(chatId, userId)
        }
    }
}

class ChatViewModelFactory(
    private val chatId: String,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: com.skinsense.ai.data.UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatId, authRepository, chatRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
