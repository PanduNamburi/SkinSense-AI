package com.skinsense.ai

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.skinsense.ai.data.FirestoreUserRepository
import com.skinsense.ai.data.FirebaseAuthRepository
import com.skinsense.ai.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SkinSenseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("SkinSenseFCM", "New token generated: $token")
        
        // Update token in Firestore if user is logged in
        val authRepository = FirebaseAuthRepository()
        val userId = authRepository.getDirectUserId()
        if (userId != null) {
            serviceScope.launch {
                val userRepository = FirestoreUserRepository()
                // We need to know the role. Since we don't have it easily here, 
                // we can fetch the profile first or just try to update in all roles.
                // In this app, we can check the profile collection.
                val profileResult = userRepository.getUserProfile(userId)
                profileResult.onSuccess { user ->
                    user?.let {
                        userRepository.updateFcmToken(userId, it.role, token)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("SkinSenseFCM", "From: ${message.from}")

        // Check if message contains a notification payload.
        message.notification?.let {
            Log.d("SkinSenseFCM", "Message Notification Body: ${it.body}")
            val title = it.title ?: "SkinSense AI"
            val body = it.body ?: ""
            
            // Get data payload if exists
            val data = message.data
            val targetScreen = data["target_screen"]
            
            notificationHelper.showNotification(title, body, targetScreen, data)
        }

        // Also handle data messages if notification is null but data is present
        if (message.notification == null && message.data.isNotEmpty()) {
            val title = message.data["title"] ?: "SkinSense AI"
            val body = message.data["body"] ?: ""
            val targetScreen = message.data["target_screen"]
            
            notificationHelper.showNotification(title, body, targetScreen, message.data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // serviceScope.cancel() // Don't cancel SupervisorJob if shared, but here it's specific to service
    }
}
