package com.skinsense.ai.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricHelper(private val activity: FragmentActivity) {

    private val biometricManager = BiometricManager.from(activity)

    fun isBiometricAvailable(): Boolean {
        // Check for both STRONG and WEAK biometrics
        val statusStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val statusWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        
        android.util.Log.d("SkinSense", "BiometricHelper: statusStrong=$statusStrong, statusWeak=$statusWeak")

        // Return true if hardware is present, regardless of enrollment
        val hardwarePresent = statusStrong != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                             statusStrong != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE &&
                             statusWeak != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                             
        return hardwarePresent
    }

    fun showBiometricPrompt(
        title: String,
        subtitle: String,
        onSuccess: (result: BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
