package com.skinsense.ai.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface NotificationRepository {
    suspend fun sendNotificationToDoctor(doctorId: String, title: String, message: String): Result<Boolean>
    suspend fun sendNotification(recipientId: String, title: String, message: String, data: Map<String, String> = emptyMap()): Result<Boolean>
}

class FirestoreNotificationRepository(private val notificationHelper: com.skinsense.ai.utils.NotificationHelper? = null) : NotificationRepository {
    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    /**
     * In a production app, this would trigger a Cloud Function via a specific collection 
     * or by calling an HTTPS endpoint. For this implementation, we'll simulate the 
     * trigger by writing to a 'notifications' collection that the backend listens to.
     */
    override suspend fun sendNotificationToDoctor(doctorId: String, title: String, message: String): Result<Boolean> {
        return sendNotification(doctorId, title, message, mapOf("target_screen" to "doctor_consultations"))
    }

    override suspend fun sendNotification(
        recipientId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): Result<Boolean> {
        return try {
            val notificationData = mutableMapOf(
                "recipientId" to recipientId,
                "title" to title,
                "message" to message,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )
            // Add extra data if any
            data.forEach { (key, value) ->
                notificationData[key] = value
            }
            
            firestore.collection("notification_triggers").add(notificationData).await()
            Log.d("SkinSense", "Notification trigger sent to recipient: $recipientId")

            // Trigger local notification if the recipient is the current user
            if (recipientId == auth.currentUser?.uid) {
                notificationHelper?.showNotification(title, message, data["target_screen"], data)
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e("SkinSense", "Failed to trigger notification: ${e.message}")
            Result.failure(e)
        }
    }
}
