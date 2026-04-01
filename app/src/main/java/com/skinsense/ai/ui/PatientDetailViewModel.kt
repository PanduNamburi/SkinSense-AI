package com.skinsense.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.AuthRepository
import com.skinsense.ai.data.ChatRepository
import com.skinsense.ai.data.Consultation
import com.skinsense.ai.data.ConsultationRepository
import com.skinsense.ai.data.User
import com.skinsense.ai.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PatientDetailViewModel(
    private val userRepository: UserRepository,
    private val consultationRepository: ConsultationRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _patient = MutableStateFlow<User?>(null)
    val patient: StateFlow<User?> = _patient.asStateFlow()

    private val _consultations = MutableStateFlow<List<Consultation>>(emptyList())
    val consultations: StateFlow<List<Consultation>> = _consultations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchPatientDetails(patientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = userRepository.getUserProfile(patientId)
                if (result.isSuccess) {
                    _patient.value = result.getOrNull()
                    fetchPatientConsultations(patientId)
                } else {
                    _error.value = "Failed to load patient profile"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchPatientConsultations(patientId: String) {
        viewModelScope.launch {
            try {
                consultationRepository.getPatientConsultations(patientId).collect {
                    _consultations.value = it
                }
            } catch (e: Exception) {
                // Non-critical, just log or ignore if history fails separately
                e.printStackTrace()
            }
        }
    }

    fun startChat(patientId: String, patientName: String, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            val doctorName = authRepository.currentUser.value ?: "Doctor"
            
            val result = chatRepository.getOrCreateChat(patientId, doctorId, patientName, doctorName)
            if (result.isSuccess) {
                result.getOrNull()?.let { chatId ->
                    onChatCreated(chatId)
                }
            } else {
                _error.value = "Failed to start chat"
            }
        }
    }
}

class PatientDetailViewModelFactory(
    private val userRepository: UserRepository,
    private val consultationRepository: ConsultationRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatientDetailViewModel(userRepository, consultationRepository, chatRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
