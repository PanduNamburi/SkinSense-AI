package com.skinsense.ai.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.skinsense.ai.data.UserRole

class SecurityManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    fun saveCredentials(email: String, password: String, role: UserRole) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PASSWORD, password)
            putString(KEY_USER_ROLE, role.name)
            apply()
        }
    }

    fun getCredentials(): Triple<String, String, UserRole>? {
        val email = sharedPreferences.getString(KEY_USER_EMAIL, null)
        val password = sharedPreferences.getString(KEY_USER_PASSWORD, null)
        val roleName = sharedPreferences.getString(KEY_USER_ROLE, null)
        
        return if (email != null && password != null && roleName != null) {
            Triple(email, password, UserRole.valueOf(roleName))
        } else {
            null
        }
    }

    fun clearCredentials() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PASSWORD)
            remove(KEY_USER_ROLE)
            apply()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
}
