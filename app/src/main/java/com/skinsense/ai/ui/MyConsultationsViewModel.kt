package com.skinsense.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.AuthRepository
import com.skinsense.ai.data.Consultation
import com.skinsense.ai.data.ConsultationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyConsultationsViewModel(
    private val authRepository: AuthRepository,
    private val consultationRepository: ConsultationRepository
) : ViewModel() {

    private val _consultations = MutableStateFlow<List<Consultation>>(emptyList())
    val consultations: StateFlow<List<Consultation>> = _consultations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchConsultations()
    }

    private fun fetchConsultations() {
        viewModelScope.launch {
            authRepository.currentUserId.collectLatest { uid ->
                if (uid != null) {
                    _isLoading.value = true
                    consultationRepository.getPatientConsultations(uid).collectLatest { list ->
                        _consultations.value = list
                        _isLoading.value = false
                    }
                }
            }
        }
    }
}

class MyConsultationsViewModelFactory(
    private val authRepository: AuthRepository,
    private val consultationRepository: ConsultationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyConsultationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyConsultationsViewModel(authRepository, consultationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
