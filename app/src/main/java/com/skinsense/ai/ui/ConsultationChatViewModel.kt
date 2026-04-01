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

class ConsultationChatViewModel(
    private val consultationId: String,
    private val authRepository: AuthRepository,
    private val consultationRepository: ConsultationRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ConsultationMessage>>(emptyList())
    val messages: StateFlow<List<ConsultationMessage>> = _messages.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    init {
        fetchMessages()
    }

    private fun fetchMessages() {
        viewModelScope.launch {
            consultationRepository.getMessages(consultationId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(text: String, type: String = "TEXT", attachmentUrl: String? = null, report: ReportAttachment? = null) {
        if (text.isBlank() && attachmentUrl == null && report == null) return
        
        viewModelScope.launch {
            val senderId = authRepository.currentUserId.value ?: return@launch
            val senderName = authRepository.currentUser.value ?: "User"
            
            val message = ConsultationMessage(
                consultationId = consultationId,
                senderId = senderId,
                senderName = senderName,
                text = text,
                messageType = type,
                attachmentUrl = attachmentUrl,
                reportAttachment = report,
                timestamp = System.currentTimeMillis()
            )
            consultationRepository.sendMessage(message)
        }
    }

    fun sendImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            val result = consultationRepository.uploadImage(context, uri)
            _isUploading.value = false
            
            result.onSuccess { url ->
                sendMessage(text = "", type = "IMAGE", attachmentUrl = url)
            }.onFailure { e ->
                _uploadError.value = "Upload failed: ${e.message}"
            }
        }
    }

    fun clearUploadError() { _uploadError.value = null }

    fun sendReport(report: ReportAttachment) {
        sendMessage(text = "", type = "REPORT", report = report)
    }
}

class ConsultationChatViewModelFactory(
    private val consultationId: String,
    private val authRepository: AuthRepository,
    private val consultationRepository: ConsultationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConsultationChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConsultationChatViewModel(consultationId, authRepository, consultationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
