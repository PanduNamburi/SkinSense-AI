package com.skinsense.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.AuthRepository
import com.skinsense.ai.data.Consultation
import com.skinsense.ai.data.ConsultationRepository
import com.skinsense.ai.data.ConsultationStatus
import com.skinsense.ai.data.Appointment
import com.skinsense.ai.data.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DoctorDashboardViewModel(
    private val authRepository: AuthRepository,
    private val consultationRepository: ConsultationRepository,
    private val userRepository: com.skinsense.ai.data.UserRepository,
    private val chatRepository: com.skinsense.ai.data.ChatRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val _pendingRequests = MutableStateFlow<List<Consultation>>(emptyList())
    val pendingRequests: StateFlow<List<Consultation>> = _pendingRequests.asStateFlow()

    private val _myConsultations = MutableStateFlow<List<Consultation>>(emptyList())
    val myConsultations: StateFlow<List<Consultation>> = _myConsultations.asStateFlow()

    private val _doctorProfile = MutableStateFlow<com.skinsense.ai.data.User?>(null)
    val doctorProfile: StateFlow<com.skinsense.ai.data.User?> = _doctorProfile.asStateFlow()

    private val _chats = MutableStateFlow<List<com.skinsense.ai.data.Chat>>(emptyList())
    val chats: StateFlow<List<com.skinsense.ai.data.Chat>> = _chats.asStateFlow()

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount = _totalUnreadCount.asStateFlow()

    // Connection Flow State
    private val _connectionRequests = MutableStateFlow<List<com.skinsense.ai.data.ConnectionRequest>>(emptyList())
    val connectionRequests: StateFlow<List<com.skinsense.ai.data.ConnectionRequest>> = _connectionRequests.asStateFlow()

    private val _connectedPatients = MutableStateFlow<List<com.skinsense.ai.data.User>>(emptyList())
    val connectedPatients: StateFlow<List<com.skinsense.ai.data.User>> = _connectedPatients.asStateFlow()

    private val _pendingAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val pendingAppointments: StateFlow<List<Appointment>> = _pendingAppointments.asStateFlow()

    private val _upcomingAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val upcomingAppointments: StateFlow<List<Appointment>> = _upcomingAppointments.asStateFlow()

    val currentUserId = authRepository.currentUserId

    init {
        fetchPendingRequests()
        fetchConnectionRequests() // New
        fetchMyConsultations()
        fetchConnectedPatients() // New
        fetchDoctorProfile()
        fetchChats()
        fetchAppointments()
        observeUnreadCounts()
    }

    private fun fetchPendingRequests() {
        viewModelScope.launch {
            consultationRepository.getPendingRequests().collect { requests ->
                _pendingRequests.value = requests
            }
        }
    }

    private fun fetchConnectionRequests() {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            val result = userRepository.getPendingConnectionRequests(doctorId)
            if (result.isSuccess) {
                _connectionRequests.value = result.getOrDefault(emptyList())
            }
        }
    }

    private fun fetchMyConsultations() {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            consultationRepository.getDoctorConsultations(doctorId).collect { consultations ->
                _myConsultations.value = consultations
            }
        }
    }

    private fun fetchConnectedPatients() {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            val result = userRepository.getConnectedPatients(doctorId)
            if (result.isSuccess) {
                _connectedPatients.value = result.getOrDefault(emptyList())
            }
        }
    }

    private fun fetchChats() {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            chatRepository.getDoctorChats(doctorId).collect { chatList ->
                _chats.value = chatList
            }
        }
    }

    private fun fetchAppointments() {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            appointmentRepository.getDoctorAppointments(doctorId).collect { allAppointments ->
                val pending = allAppointments.filter { it.status == "pending" }
                val confirmed = allAppointments.filter { it.status == "confirmed" }
                _pendingAppointments.value = pending
                _upcomingAppointments.value = confirmed
            }
        }
    }

    fun acceptAppointment(appointmentId: String) {
        viewModelScope.launch {
            appointmentRepository.updateAppointmentStatus(appointmentId, "confirmed")
        }
    }

    fun declineAppointment(appointmentId: String) {
        viewModelScope.launch {
            appointmentRepository.updateAppointmentStatus(appointmentId, "cancelled")
        }
    }

    fun acceptRequest(consultation: Consultation) {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            val doctorName = authRepository.currentUser.value ?: "Doctor"
            consultationRepository.updateConsultationStatus(
                consultation.id, 
                ConsultationStatus.ACCEPTED, 
                doctorId, 
                doctorName
            )
        }
    }

    fun approveConnectionRequest(request: com.skinsense.ai.data.ConnectionRequest) {
        viewModelScope.launch {
            val result = userRepository.respondToConnectionRequest(request.id, com.skinsense.ai.data.ConnectionStatus.ACCEPTED)
            if (result.isSuccess) {
                // Refresh lists
                fetchConnectionRequests()
                fetchConnectedPatients()
            }
        }
    }

    fun rejectConnectionRequest(request: com.skinsense.ai.data.ConnectionRequest) {
        viewModelScope.launch {
            val result = userRepository.respondToConnectionRequest(request.id, com.skinsense.ai.data.ConnectionStatus.REJECTED)
            if (result.isSuccess) {
                fetchConnectionRequests()
            }
        }
    }

    private fun fetchDoctorProfile() {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            val result = userRepository.getUserProfile(doctorId)
            if (result.isSuccess) {
                _doctorProfile.value = result.getOrNull()
            }
        }
    }

    private fun observeUnreadCounts() {
        viewModelScope.launch {
            val doctorId = authRepository.currentUserId.value ?: return@launch
            chatRepository.getTotalUnreadCount(doctorId).collect { count ->
                _totalUnreadCount.value = count
            }
        }
    }
}

class DoctorDashboardViewModelFactory(
    private val authRepository: AuthRepository,
    private val consultationRepository: ConsultationRepository,
    private val userRepository: com.skinsense.ai.data.UserRepository,
    private val chatRepository: com.skinsense.ai.data.ChatRepository,
    private val appointmentRepository: AppointmentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoctorDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DoctorDashboardViewModel(authRepository, consultationRepository, userRepository, chatRepository, appointmentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
