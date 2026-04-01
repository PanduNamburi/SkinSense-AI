package com.skinsense.ai.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.User
import com.skinsense.ai.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminDashboardViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _pendingDoctors = MutableStateFlow<List<User>>(emptyList())
    val pendingDoctors: StateFlow<List<User>> = _pendingDoctors.asStateFlow()

    private val _allDoctors = MutableStateFlow<List<User>>(emptyList())
    val allDoctors: StateFlow<List<User>> = _allDoctors.asStateFlow()

    private val _allPatients = MutableStateFlow<List<User>>(emptyList())
    val allPatients: StateFlow<List<User>> = _allPatients.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val pending = userRepository.getUnverifiedDoctors()
                _pendingDoctors.value = pending.getOrDefault(emptyList())

                val doctors = userRepository.getAllDoctors()
                _allDoctors.value = doctors.getOrDefault(emptyList())

                val patients = userRepository.getAllPatients()
                _allPatients.value = patients.getOrDefault(emptyList())
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveDoctor(uid: String) {
        viewModelScope.launch {
            try {
                val result = userRepository.verifyDoctor(uid)
                if (result.isSuccess) {
                    _successMessage.value = "Doctor approved successfully!"
                    loadAllData()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to approve doctor"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun rejectDoctor(uid: String) {
        viewModelScope.launch {
            try {
                val result = userRepository.setDoctorStatus(uid, "rejected")
                if (result.isSuccess) {
                    _successMessage.value = "Doctor rejected."
                    loadAllData()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun setDoctorActive(uid: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val result = userRepository.setUserActive(uid, "doctors", isActive)
                if (result.isSuccess) {
                    _successMessage.value = if (isActive) "Doctor activated." else "Doctor deactivated."
                    loadAllData()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun setPatientActive(uid: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val result = userRepository.setUserActive(uid, "patients", isActive)
                if (result.isSuccess) {
                    _successMessage.value = if (isActive) "Patient activated." else "Patient deactivated."
                    loadAllData()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}

class AdminDashboardViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminDashboardViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
