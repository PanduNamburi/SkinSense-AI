package com.skinsense.ai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.AppDatabase
import com.skinsense.ai.data.AuthRepository
import com.skinsense.ai.data.DiseaseRepository
import com.skinsense.ai.data.HistoryEntity
import com.skinsense.ai.ui.compose.HistoryItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class HistoryViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val firestoreHistoryRepository: com.skinsense.ai.data.FirestoreHistoryRepository = com.skinsense.ai.data.FirestoreHistoryRepository()
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val historyDao = database.historyDao()

    // Observe current user ID and switch the history flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: StateFlow<List<HistoryItem>> = authRepository.currentUserId
        .onEach { android.util.Log.d("HistoryViewModel", "Current UserId emitted: $it") }
        .flatMapLatest { userId ->
            if (userId != null) {
                android.util.Log.d("HistoryViewModel", "Subscribing to Firestore history for userId: $userId")
                firestoreHistoryRepository.getHistory(userId)
            } else {
                android.util.Log.d("HistoryViewModel", "UserId is null, returning empty list")
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun getHistoryItemById(id: String): HistoryItem? {
        val userId = authRepository.currentUserId.value ?: return null
        return firestoreHistoryRepository.getHistoryItem(userId, id)
    }
    
    fun deleteHistoryItem(item: HistoryItem) {
        val userId = authRepository.currentUserId.value ?: return
        viewModelScope.launch {
            try {
                // Delete from Firestore
                firestoreHistoryRepository.deleteHistory(userId, item.id)
                // Delete from local
                try {
                    historyDao.deleteById(item.id.toInt())
                } catch (e: Exception) {
                    // Ignore if ID format doesn't match
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error deleting item", e)
            }
        }
    }

    fun clearHistory() {
        val userId = authRepository.currentUserId.value ?: return
        viewModelScope.launch {
            try {
                // Clear from Firestore
                firestoreHistoryRepository.deleteAllHistory(userId)
                // Clear from local Room
                historyDao.deleteAllForUser(userId)
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Error clearing history", e)
            }
        }
    }
}

class HistoryViewModelFactory(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val firestoreHistoryRepository: com.skinsense.ai.data.FirestoreHistoryRepository = com.skinsense.ai.data.FirestoreHistoryRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(application, authRepository, firestoreHistoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
