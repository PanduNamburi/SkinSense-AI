package com.skinsense.ai.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for Authentication Repository
 * Uses Email/Password authentication only (no OTP/Phone auth)
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Boolean>
    suspend fun signUp(name: String, email: String, password: String): Result<Boolean>
    fun signOut()
    fun isUserLoggedIn(): Boolean
    val currentUser: StateFlow<String?>
    val currentUserId: StateFlow<String?>
    fun getCurrentUserEmail(): String?
    fun getDirectUserId(): String?   // Synchronous read — avoids StateFlow race condition
    suspend fun deleteAccount(): Result<Boolean>
}
