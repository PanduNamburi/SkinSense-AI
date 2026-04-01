package com.skinsense.ai.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.AuthRepository
import com.skinsense.ai.data.FirebaseAuthRepository
import com.skinsense.ai.data.User
import com.skinsense.ai.data.UserRepository
import com.skinsense.ai.data.UserRole
import com.skinsense.ai.utils.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object PendingApproval : AuthState() // Doctor pending
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val userRepository: UserRepository? = null,
    private val securityManager: SecurityManager? = null
) : ViewModel() {

    private val _isBiometricAvailable = MutableStateFlow(false)
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _showBiometricSetupPrompt = MutableStateFlow(false)
    val showBiometricSetupPrompt: StateFlow<Boolean> = _showBiometricSetupPrompt.asStateFlow()

    private var pendingCredentials: Pair<String, String>? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUserRole = MutableStateFlow<UserRole?>(null)
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    init {
        checkBiometricStatus()
        viewModelScope.launch {
            authRepository.currentUserId.collect { uid ->
                if (uid != null && userRepository != null) {
                    loadUserProfile(uid)
                } else if (uid == null) {
                    _currentUserRole.value = null
                    _userProfile.value = null
                }
            }
        }
    }

    fun refreshProfile() {
        val uid = authRepository.currentUserId.value
        if (uid != null && userRepository != null) {
            viewModelScope.launch {
                loadUserProfile(uid)
            }
        }
    }

    private suspend fun loadUserProfile(uid: String) {
        try {
            val result = userRepository?.getUserProfile(uid)
            val user = result?.getOrNull()
            _userProfile.value = user

            if (user == null) {
                _currentUserRole.value = null
                return
            }

            // For doctors, gate access based on their approval status
            if (user.role == UserRole.DOCTOR) {
                when (user.status) {
                    "pending_approval" -> {
                        // Do NOT set role — keeps LaunchedEffect from routing to DoctorDashboard
                        _currentUserRole.value = null
                        // Only update authState if we're not mid-signup (avoid overwriting Loading state)
                        if (_authState.value !is AuthState.Loading) {
                            _authState.value = AuthState.PendingApproval
                        }
                        authRepository.signOut()
                        Log.d("SkinSense", "AuthViewModel: Doctor pending approval — signed out")
                        return
                    }
                    "rejected" -> {
                        _currentUserRole.value = null
                        _authState.value = AuthState.Error("Your application was rejected. Please contact support.")
                        authRepository.signOut()
                        return
                    }
                }
                // Deactivated check
                if (!user.isActive) {
                    _currentUserRole.value = null
                    _authState.value = AuthState.Error("Your account has been deactivated. Please contact support.")
                    authRepository.signOut()
                    return
                }
            }

            _currentUserRole.value = user.role
            // If we're loading a valid existing profile, the user is effectively "logged in successfully"
            if (_authState.value == AuthState.Idle || _authState.value == AuthState.Loading) {
                _authState.value = AuthState.Success
            }
            Log.d("SkinSense", "AuthViewModel: Profile loaded, role=${user.role}")
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Failed to load profile")
            Log.e("SkinSense", "AuthViewModel: Failed to load profile: ${e.message}")
        }
    }

    /**
     * Role-aware login: validates the selected role matches the user's stored role
     */
    fun loginWithRole(email: String, password: String, selectedRole: UserRole) {
        Log.d("SkinSense", "AuthViewModel: loginWithRole called for $email, role=$selectedRole")
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Authenticate with Firebase
                val loginResult = authRepository.login(email, password)
                if (loginResult.isFailure) {
                    _authState.value = AuthState.Error(
                        loginResult.exceptionOrNull()?.message ?: "Login failed"
                    )
                    return@launch
                }

                // Load Firestore profile
                val uid = authRepository.currentUserId.value
                if (uid == null) {
                    _authState.value = AuthState.Error("Authentication error. Please try again.")
                    return@launch
                }

                val profileResult = userRepository?.getUserProfile(uid)
                if (profileResult?.isFailure == true) {
                    val error = profileResult.exceptionOrNull()?.message ?: "Failed to fetch user profile"
                    Log.e("SkinSense", "AuthViewModel: Profile fetch failed: $error")
                    _authState.value = AuthState.Error("Connection error: $error")
                    return@launch
                }

                val user = profileResult?.getOrNull()

                if (user == null) {
                    // New user — create a basic profile based on selected role
                    Log.d("SkinSense", "AuthViewModel: No profile found for $uid, creating new as $selectedRole")
                    val newUser = User(
                        uid = uid,
                        email = email,
                        displayName = authRepository.currentUser.value ?: "",
                        role = selectedRole
                    )
                    val createResult = userRepository?.createUserProfile(newUser)
                    if (createResult?.isFailure == true) {
                        _authState.value = AuthState.Error("Failed to create profile: ${createResult.exceptionOrNull()?.message}")
                        return@launch
                    }
                    _userProfile.value = newUser
                    _currentUserRole.value = selectedRole
                    _authState.value = AuthState.Success
                    return@launch
                }

                // Validate role matches
                if (user.role != selectedRole) {
                    authRepository.signOut()
                    _authState.value = AuthState.Error(
                        "This account is registered as a ${user.role.name.lowercase()}. Please select the correct role."
                    )
                    return@launch
                }

                // Check doctor status
                if (user.role == UserRole.DOCTOR && user.status == "pending_approval") {
                    _userProfile.value = user
                    _currentUserRole.value = user.role
                    _authState.value = AuthState.PendingApproval
                    return@launch
                }

                // Check doctor rejected
                if (user.role == UserRole.DOCTOR && user.status == "rejected") {
                    authRepository.signOut()
                    _authState.value = AuthState.Error("Your account has been rejected. Please contact support.")
                    return@launch
                }

                // Check inactive
                if (!user.isActive) {
                    authRepository.signOut()
                    _authState.value = AuthState.Error("Your account has been deactivated. Please contact support.")
                    return@launch
                }

                _userProfile.value = user
                _currentUserRole.value = user.role
                _authState.value = AuthState.Success
                Log.d("SkinSense", "AuthViewModel: Login success, role=${user.role}")

                // Post-login biometric setup check
                if (securityManager != null && !securityManager.isBiometricEnabled()) {
                    pendingCredentials = email to password
                    _showBiometricSetupPrompt.value = true
                } else if (securityManager != null && securityManager.isBiometricEnabled()) {
                    // Refresh stored password if it changed
                    securityManager.saveCredentials(email, password, user.role)
                }

            } catch (e: Exception) {
                Log.e("SkinSense", "AuthViewModel: Login error: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    /**
     * Patient signup
     */
    fun signUpPatient(
        name: String, email: String, password: String,
        phone: String, age: String, gender: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val signUpResult = authRepository.signUp(name, email, password)
                if (signUpResult.isFailure) {
                    _authState.value = AuthState.Error(
                        signUpResult.exceptionOrNull()?.message ?: "Sign up failed"
                    )
                    return@launch
                }
                // Use direct read — avoids race condition where StateFlow hasn't updated yet
                val uid = authRepository.getDirectUserId() ?: run {
                    _authState.value = AuthState.Error("Session error. Please try again.")
                    return@launch
                }
                val patientUser = User(
                    uid = uid,
                    email = email,
                    displayName = name,
                    role = UserRole.PATIENT,
                    phone = phone,
                    dateOfBirth = age, // Storing Age in dateOfBirth for now, Profile will handle it
                    gender = gender,
                    isVerified = true,
                    isActive = true
                )
                val profileResult = userRepository?.createUserProfile(patientUser)
                Log.d("SkinSense", "signUpPatient: profileResult=${profileResult?.isSuccess}, uid=$uid")
                if (profileResult?.isFailure == true) {
                    Log.e("SkinSense", "signUpPatient: Firestore write failed: ${profileResult.exceptionOrNull()?.message}")
                }
                // Even if Firestore write fails, auth succeeded — proceed to home, profile will retry on next login
                _userProfile.value = patientUser
                _currentUserRole.value = UserRole.PATIENT
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("SkinSense", "signUpPatient error: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    /**
     * Doctor signup (creates account with "pending_approval" status)
     */
    fun signUpDoctor(
        name: String, email: String, password: String,
        specialization: String, qualifications: String,
        experienceYears: Int, hospitalName: String,
        contactDetails: String, licenseNumber: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val signUpResult = authRepository.signUp(name, email, password)
                if (signUpResult.isFailure) {
                    _authState.value = AuthState.Error(
                        signUpResult.exceptionOrNull()?.message ?: "Sign up failed"
                    )
                    return@launch
                }
                // Use direct read — avoids race condition where StateFlow hasn't updated yet
                val uid = authRepository.getDirectUserId() ?: run {
                    _authState.value = AuthState.Error("Session error. Please try again.")
                    return@launch
                }
                val doctorUser = User(
                    uid = uid,
                    email = email,
                    displayName = name,
                    role = UserRole.DOCTOR,
                    specialization = specialization,
                    qualifications = qualifications,
                    experienceYears = experienceYears,
                    hospitalName = hospitalName,
                    contactDetails = contactDetails,
                    licenseNumber = licenseNumber,
                    isVerified = false,
                    status = "pending_approval",
                    isActive = true
                )
                userRepository?.createUserProfile(doctorUser)
                // After doctor signup, sign out (they must wait for approval)
                authRepository.signOut()
                _userProfile.value = null
                _currentUserRole.value = null
                _authState.value = AuthState.PendingApproval
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Idle
        _currentUserRole.value = null
        _userProfile.value = null
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val uid = authRepository.currentUserId.value ?: return@launch
            _authState.value = AuthState.Loading
            try {
                kotlinx.coroutines.withTimeoutOrNull(10000) {
                    userRepository?.deleteUser(uid)
                }
                kotlinx.coroutines.withTimeoutOrNull(10000) {
                    authRepository.deleteAccount()
                }
                signOut()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to delete account")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun checkBiometricStatus() {
        _isBiometricEnabled.value = securityManager?.isBiometricEnabled() ?: false
    }

    fun updateBiometricAvailability(available: Boolean) {
        _isBiometricAvailable.value = available
    }

    fun enableBiometric(enabled: Boolean) {
        if (enabled && pendingCredentials != null && securityManager != null) {
            securityManager.saveCredentials(
                pendingCredentials!!.first,
                pendingCredentials!!.second,
                _currentUserRole.value ?: UserRole.PATIENT
            )
            securityManager.setBiometricEnabled(true)
            _isBiometricEnabled.value = true
        }
        _showBiometricSetupPrompt.value = false
        pendingCredentials = null
    }

    fun loginWithBiometric() {
        val credentials = securityManager?.getCredentials()
        if (credentials != null) {
            loginWithRole(credentials.first, credentials.second, credentials.third)
        } else {
            _authState.value = AuthState.Error("No biometric credentials found. Please sign in with your password.")
        }
    }

    fun disableBiometric() {
        securityManager?.clearCredentials()
        securityManager?.setBiometricEnabled(false)
        _isBiometricEnabled.value = false
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val securityManager: SecurityManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository, userRepository, securityManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
