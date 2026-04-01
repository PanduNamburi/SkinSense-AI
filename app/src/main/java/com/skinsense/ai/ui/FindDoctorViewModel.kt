package com.skinsense.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import android.util.Log
import kotlinx.coroutines.launch

class FindDoctorViewModel(
    private val userRepository: UserRepository,
    private val consultationRepository: ConsultationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _doctors = MutableStateFlow<List<User>>(emptyList())
    val doctors: StateFlow<List<User>> = _doctors.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _connectionStatuses = MutableStateFlow<Map<String, ConnectionStatus>>(emptyMap())
    val connectionStatuses: StateFlow<Map<String, ConnectionStatus>> = _connectionStatuses.asStateFlow()

    private val _selectedSpecialization = MutableStateFlow("All")
    val selectedSpecialization: StateFlow<String> = _selectedSpecialization.asStateFlow()

    private val _specializations = MutableStateFlow<List<String>>(listOf("All"))
    val specializations: StateFlow<List<String>> = _specializations.asStateFlow()

    init {
        Log.d("SkinSense", "FindDoctorViewModel init")
    }

    fun fetchDoctors() {
        val vmHash = System.identityHashCode(this)
        Log.d("SkinSense", "FindDoctorViewModel [$vmHash] fetchDoctors called")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("SkinSense", "FindDoctorViewModel [$vmHash] calling userRepository.getDoctors")
                val result = userRepository.getDoctors()
                if (result.isSuccess) {
                    val docs = result.getOrDefault(emptyList())
                    Log.d("SkinSense", "FindDoctorViewModel [$vmHash] success, docs count: ${docs.size}")
                    _doctors.value = docs
                    updateSpecializations(docs)
                    fetchConnectionStatuses(docs)
                } else {
                    Log.e("SkinSense", "FindDoctorViewModel [$vmHash] error: ${result.exceptionOrNull()?.message}")
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to fetch doctors"
                }
            } catch (e: Exception) {
                Log.e("SkinSense", "FindDoctorViewModel [$vmHash] catch - error: ${e.message}")
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
                Log.d("SkinSense", "FindDoctorViewModel [$vmHash] fetchDoctors finally - isLoading set to false")
            }
        }
    }

    fun fetchDoctorById(doctorId: String) {
        val vmHash = System.identityHashCode(this)
        Log.d("SkinSense", "FindDoctorViewModel [$vmHash] fetchDoctorById called for $doctorId")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = userRepository.getUserProfile(doctorId)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null && user.role == UserRole.DOCTOR) {
                        // Update the list with this doctor if not already there
                        val currentList = _doctors.value.toMutableList()
                        if (currentList.none { it.uid == user.uid }) {
                            currentList.add(user)
                            _doctors.value = currentList
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SkinSense", "FindDoctorViewModel [$vmHash] fetchDoctorById error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestConsultation(doctor: User, analysisResult: AnalysisResult) {
        viewModelScope.launch {
            val patientId = authRepository.currentUserId.value ?: return@launch
            val patientName = authRepository.currentUser.value ?: "Patient"
            
            val consultation = Consultation(
                patientId = patientId,
                patientName = patientName,
                doctorId = doctor.uid,
                doctorName = doctor.displayName,
                diseaseName = analysisResult.diseaseName,
                confidence = analysisResult.confidence,
                imageUri = analysisResult.imageUri.toString(),
                status = ConsultationStatus.PENDING
            )
            
            consultationRepository.requestConsultation(consultation)
        }
    }

    fun sendConnectionRequest(doctor: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val patientId = authRepository.currentUserId.value
            val patientName = authRepository.currentUser.value ?: "Patient"
            
            if (patientId == null) {
                onResult(false, "You must be logged in")
                return@launch
            }

            try {
                val result = userRepository.sendConnectionRequest(patientId, doctor.uid, patientName)
                if (result.isSuccess) {
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }



    private fun updateSpecializations(docs: List<User>) {
        val specs = docs.mapNotNull { it.specialization }.distinct().sorted().toMutableList()
        specs.add(0, "All")
        _specializations.value = specs
    }

    fun selectSpecialization(specialization: String) {
        _selectedSpecialization.value = specialization
    }

    private fun fetchConnectionStatuses(docs: List<User>) {
        viewModelScope.launch {
            val patientId = authRepository.currentUserId.value ?: return@launch
            val statuses = mutableMapOf<String, ConnectionStatus>()
            docs.forEach { doctor ->
                val status = userRepository.getConnectionStatus(patientId, doctor.uid)
                if (status != null) {
                    statuses[doctor.uid] = status
                }
            }
            _connectionStatuses.value = statuses
        }
    }

    fun refreshConnectionStatus(doctorId: String) {
        viewModelScope.launch {
            val patientId = authRepository.currentUserId.value ?: return@launch
            val status = userRepository.getConnectionStatus(patientId, doctorId)
            if (status != null) {
                val currentStatuses = _connectionStatuses.value.toMutableMap()
                currentStatuses[doctorId] = status
                _connectionStatuses.value = currentStatuses
            }
        }
    }
}

class FindDoctorViewModelFactory(
    private val userRepository: UserRepository,
    private val consultationRepository: ConsultationRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FindDoctorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FindDoctorViewModel(userRepository, consultationRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
