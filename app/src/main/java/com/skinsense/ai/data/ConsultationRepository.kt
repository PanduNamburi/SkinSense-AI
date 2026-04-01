package com.skinsense.ai.data

import android.util.Log
import android.util.Base64
import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface ConsultationRepository {
    suspend fun requestConsultation(consultation: Consultation): Result<Boolean>
    fun getPendingRequests(): Flow<List<Consultation>>
    fun getDoctorConsultations(doctorId: String): Flow<List<Consultation>>
    fun getPatientConsultations(patientId: String): Flow<List<Consultation>>
    suspend fun updateConsultationStatus(id: String, status: ConsultationStatus, doctorId: String, doctorName: String): Result<Boolean>
    fun getMessages(consultationId: String): Flow<List<ConsultationMessage>>
    suspend fun sendMessage(message: ConsultationMessage): Result<Boolean>
    suspend fun uploadImage(context: Context, uri: Uri): Result<String>
    suspend fun updatePaymentStatus(id: String, paymentStatus: String, paymentId: String): Result<Boolean>
}

class FirestoreConsultationRepository(
    private val notificationRepository: NotificationRepository? = null
) : ConsultationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val consultationsCollection = firestore.collection("consultations")
    private val messagesCollection = firestore.collection("consultation_messages")
    private val imgbbApiKey = com.skinsense.ai.BuildConfig.IMGBB_API_KEY

    /** Save status as its string name to avoid Kotlin enum ordinal serialization issues */
    override suspend fun requestConsultation(consultation: Consultation): Result<Boolean> {
        return try {
            val docRef = consultationsCollection.document()
            val newConsultation = consultation.copy(id = docRef.id)
            // Explicit map so ConsultationStatus enum is stored as a string, not an integer
            val dataMap = mapOf(
                "id" to newConsultation.id,
                "patientId" to newConsultation.patientId,
                "doctorId" to newConsultation.doctorId,
                "status" to newConsultation.status.name,   // "PENDING", "ACCEPTED", etc.
                "imageUri" to newConsultation.imageUri,
                "diseaseName" to newConsultation.diseaseName,
                "confidence" to newConsultation.confidence,
                "timestamp" to newConsultation.timestamp,
                "patientName" to newConsultation.patientName,
                "doctorName" to newConsultation.doctorName,
                "paymentStatus" to newConsultation.paymentStatus,
                "paymentId" to newConsultation.paymentId
            )
            docRef.set(dataMap).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SkinSense", "requestConsultation failed: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getPendingRequests(): Flow<List<Consultation>> = callbackFlow {
        val subscription = consultationsCollection
            .whereEqualTo("status", ConsultationStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val requests = snapshot?.documents
                    ?.mapNotNull { snapshotToConsultation(it) }
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()
                trySend(requests)
            }
        awaitClose { subscription.remove() }
    }

    override fun getDoctorConsultations(doctorId: String): Flow<List<Consultation>> = callbackFlow {
        val subscription = consultationsCollection
            .whereEqualTo("doctorId", doctorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val consultations = snapshot?.documents
                    ?.mapNotNull { snapshotToConsultation(it) }
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()
                trySend(consultations)
            }
        awaitClose { subscription.remove() }
    }

    override fun getPatientConsultations(patientId: String): Flow<List<Consultation>> = callbackFlow {
        val subscription = consultationsCollection
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val consultations = snapshot?.documents
                    ?.mapNotNull { snapshotToConsultation(it) }
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()
                trySend(consultations)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateConsultationStatus(
        id: String,
        status: ConsultationStatus,
        doctorId: String,
        doctorName: String
    ): Result<Boolean> {
        return try {
            consultationsCollection.document(id).update(
                mapOf(
                    "status" to status.name,   // always write as string
                    "doctorId" to doctorId,
                    "doctorName" to doctorName
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SkinSense", "updateConsultationStatus failed: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getMessages(consultationId: String): Flow<List<ConsultationMessage>> = callbackFlow {
        val subscription = messagesCollection
            .whereEqualTo("consultationId", consultationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val messages = snapshot?.toObjects(ConsultationMessage::class.java)
                    ?.sortedBy { it.timestamp }
                    ?: emptyList()
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun sendMessage(message: ConsultationMessage): Result<Boolean> {
        return try {
            val docRef = messagesCollection.document()
            val newMessage = message.copy(id = docRef.id)
            docRef.set(newMessage).await()
            
            // Trigger notification
            try {
                val consultationSnapshot = consultationsCollection.document(message.consultationId).get().await()
                val patientId = consultationSnapshot.getString("patientId")
                val doctorId = consultationSnapshot.getString("doctorId")
                val patientName = consultationSnapshot.getString("patientName")
                val doctorName = consultationSnapshot.getString("doctorName")
                
                val recipientId = if (message.senderId == patientId) doctorId else patientId
                val senderName = if (message.senderId == patientId) patientName else "Dr. $doctorName"
                
                if (recipientId != null) {
                    val preview = when (message.messageType) {
                        "IMAGE" -> "sent an image"
                        "REPORT" -> "sent a report"
                        else -> message.text
                    }
                    
                    notificationRepository?.sendNotification(
                        recipientId = recipientId,
                        title = senderName ?: "New Message",
                        message = preview,
                        data = mapOf(
                            "target_screen" to "consultation_chat",
                            "consultationId" to message.consultationId
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("SkinSense", "Notification trigger failed for chat: ${e.message}")
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e("SkinSense", "sendMessage failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updatePaymentStatus(id: String, paymentStatus: String, paymentId: String): Result<Boolean> {
        return try {
            consultationsCollection.document(id).update(
                mapOf(
                    "paymentStatus" to paymentStatus,
                    "paymentId" to paymentId
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SkinSense", "updatePaymentStatus failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(context: Context, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(Exception("Cannot open image. Please try another photo."))
                val imageBytes = inputStream.use { it.readBytes() }
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                val connection = URL("https://api.imgbb.com/1/upload?key=$imgbbApiKey")
                    .openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connectTimeout = 30_000
                    readTimeout = 30_000
                }
                val postBody = "image=${URLEncoder.encode(base64Image, "UTF-8")}"
                connection.outputStream.use { it.write(postBody.toByteArray()) }

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    val errBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                    Log.e("ConsultationRepository", "ImgBB HTTP $responseCode: $errBody")
                    return@withContext Result.failure(Exception("Upload failed (HTTP $responseCode). Check API key."))
                }

                val responseBody = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(responseBody)
                val imageUrl = json.getJSONObject("data").getString("url")
                Log.d("ConsultationRepository", "uploadImage success: $imageUrl")
                Result.success(imageUrl)
            } catch (e: Exception) {
                Log.e("ConsultationRepository", "uploadImage failed: ${e.message}", e)
                Result.failure(Exception("Upload failed: ${e.message}"))
            }
        }
    }

    /** Safely deserializes a Firestore snapshot to a Consultation, mapping status string → enum */
    private fun snapshotToConsultation(snapshot: DocumentSnapshot): Consultation? {
        return try {
            val statusStr = snapshot.getString("status") ?: "PENDING"
            val status = try { ConsultationStatus.valueOf(statusStr) } catch (e: Exception) { ConsultationStatus.PENDING }
            Consultation(
                id = snapshot.getString("id") ?: snapshot.id,
                patientId = snapshot.getString("patientId") ?: "",
                doctorId = snapshot.getString("doctorId"),
                status = status,
                imageUri = snapshot.getString("imageUri") ?: "",
                diseaseName = snapshot.getString("diseaseName") ?: "",
                confidence = snapshot.getDouble("confidence")?.toFloat() ?: 0f,
                timestamp = snapshot.getLong("timestamp") ?: 0L,
                patientName = snapshot.getString("patientName") ?: "",
                doctorName = snapshot.getString("doctorName"),
                paymentStatus = snapshot.getString("paymentStatus") ?: "pending",
                paymentId = snapshot.getString("paymentId")
            )
        } catch (e: Exception) {
            Log.e("SkinSense", "snapshotToConsultation failed: ${e.message}")
            null
        }
    }
}
