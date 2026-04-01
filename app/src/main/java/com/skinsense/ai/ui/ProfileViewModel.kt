package com.skinsense.ai.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.User
import com.skinsense.ai.data.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile = _userProfile.asStateFlow()

    fun loadProfile(uid: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = userRepository.getUserProfile(uid)
            result.onSuccess { user ->
                _userProfile.value = user
                _uiState.value = ProfileUiState.Idle
            }.onFailure {
                _uiState.value = ProfileUiState.Error(it.message ?: "Failed to load profile") { loadProfile(uid) }
            }
        }
    }

    fun updateProfile(user: User) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Updating
            val result = userRepository.updateUserProfile(user)
            result.onSuccess {
                _userProfile.value = user
                _uiState.value = ProfileUiState.Success
            }.onFailure {
                _uiState.value = ProfileUiState.Error(it.message ?: "Failed to update profile") { updateProfile(user) }
            }
        }
    }

    fun resetState() {
        _uiState.value = ProfileUiState.Idle
    }

    fun uploadProfilePhoto(uid: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Updating
            
            val base64Image = uriToBase64(context, uri)
            
            if (base64Image != null) {
                val currentUser = _userProfile.value
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(profileImageUri = base64Image)
                    updateProfile(updatedUser)
                } else {
                    // If profile not loaded yet, try to load it first
                    val profileLoadResult = userRepository.getUserProfile(uid)
                    profileLoadResult.onSuccess { user ->
                        if (user != null) {
                            val updatedUser = user.copy(profileImageUri = base64Image)
                            updateProfile(updatedUser)
                        } else {
                            _uiState.value = ProfileUiState.Error("User profile not found")
                        }
                    }.onFailure { error ->
                        _uiState.value = ProfileUiState.Error(error.message ?: "Failed to load profile for upload")
                    }
                }
            } else {
                _uiState.value = ProfileUiState.Error("Failed to process image")
            }
        }
    }

    private suspend fun uriToBase64(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) return@withContext null

                // Resize to 200x200 max while maintaining aspect ratio
                val maxDimension = 200
                val scale = Math.min(
                    maxDimension.toFloat() / originalBitmap.width,
                    maxDimension.toFloat() / originalBitmap.height
                ).coerceAtMost(1.0f) // Don't scale up

                val matrix = Matrix()
                matrix.postScale(scale, scale)
                val resizedBitmap = Bitmap.createBitmap(
                    originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                )

                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val byteArray = outputStream.toByteArray()
                
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                val result = "data:image/jpeg;base64," + base64String
                Log.d("SkinSense", "Image processed. Byte size: ${byteArray.size}, Base64 length: ${base64String.length}")
                Log.d("SkinSense", "Base64 prefix: ${result.take(50)}")
                result
            } catch (e: Exception) {
                Log.e("SkinSense", "uriToBase64 failed", e)
                null
            }
        }
    }
}

class ProfileViewModelFactory(
    private val userRepository: UserRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    object Updating : ProfileUiState()
    object Success : ProfileUiState()
    data class Error(val message: String, val lastAction: (() -> Unit)? = null) : ProfileUiState()
}
