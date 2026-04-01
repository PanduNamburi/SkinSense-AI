package com.skinsense.ai.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface ChatRepository {
    suspend fun getOrCreateChat(patientId: String, doctorId: String, patientName: String, doctorName: String): Result<String>
    suspend fun sendMessage(chatId: String, message: ChatMessage, senderId: String): Result<Boolean>
    suspend fun uploadImage(context: Context, uri: Uri): Result<String>
    fun getMessages(chatId: String): Flow<List<ChatMessage>>
    fun getDoctorChats(doctorId: String): Flow<List<Chat>>
    fun getPatientChats(patientId: String): Flow<List<Chat>>
    suspend fun markChatAsRead(chatId: String, userId: String): Result<Boolean>
    fun getTotalUnreadCount(userId: String): Flow<Int>
}

class FirestoreChatRepository(
    private val notificationRepository: NotificationRepository? = null
) : ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection("chats")
    private val imgbbApiKey = com.skinsense.ai.BuildConfig.IMGBB_API_KEY

    override suspend fun getOrCreateChat(
        patientId: String,
        doctorId: String,
        patientName: String,
        doctorName: String
    ): Result<String> {
        return try {
            // Check if chat already exists
            val existingChats = chatsCollection
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()

            if (!existingChats.isEmpty) {
                return Result.success(existingChats.documents[0].id)
            }

            // Create new chat
            val newChatRef = chatsCollection.document()
            val chat = Chat(
                chatId = newChatRef.id,
                patientId = patientId,
                doctorId = doctorId,
                patientName = patientName,
                doctorName = doctorName,
                lastMessage = "",
                lastMessageTime = System.currentTimeMillis(),
                participants = listOf(patientId, doctorId),
                unreadCounts = mapOf(patientId to 0L, doctorId to 0L)
            )
            newChatRef.set(chat).await()
            Result.success(newChatRef.id)
        } catch (e: Exception) {
            Log.e("ChatRepository", "getOrCreateChat failed", e)
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(chatId: String, message: ChatMessage, senderId: String): Result<Boolean> {
        return try {
            val chatRef = chatsCollection.document(chatId)
            
            // Fetch chat to find recipient
            val chatSnapshot = chatRef.get().await()
            val chat = chatSnapshot.toObject(Chat::class.java)
            val recipientId = chat?.participants?.firstOrNull { it != senderId }
            val hasUnreadCounts = chatSnapshot.contains("unreadCounts")

            
            Log.d("ChatRepository", "sendMessage: chatId=$chatId, senderId=$senderId, recipientId=$recipientId, hasUnreadCounts=$hasUnreadCounts")

            val messagesRef = chatRef.collection("messages")
            val newMessageRef = messagesRef.document()
            
            val finalMessage = message.copy(messageId = newMessageRef.id)

            firestore.runBatch { batch ->
                // 1. Add message to subcollection
                batch.set(newMessageRef, finalMessage)
                
                // 2. Update parent chat
                val lastMessageSnippet = when (finalMessage.messageType) {
                    ChatMessageType.IMAGE -> "[Image]"
                    ChatMessageType.REPORT -> "[Report: ${finalMessage.reportAttachment?.diseaseName}]"
                    else -> finalMessage.messageText
                }

                if (hasUnreadCounts && recipientId != null) {
                    // Optimized path: Field exists, use exact UPDATE with dot notation
                    batch.update(
                        chatRef,
                        "lastMessage", lastMessageSnippet,
                        "lastMessageTime", finalMessage.timestamp,
                        "unreadCounts.$recipientId", com.google.firebase.firestore.FieldValue.increment(1)
                    )
                } else {
                    // Fallback path: Field missing OR recipient unknown
                    // Use SET with Merge to create the map if needed
                    val updates = mutableMapOf<String, Any>(
                        "lastMessage" to lastMessageSnippet,
                        "lastMessageTime" to finalMessage.timestamp
                    )
                    
                    if (recipientId != null) {
                        updates["unreadCounts"] = mapOf(recipientId to com.google.firebase.firestore.FieldValue.increment(1))
                    }
                    
                    batch.set(chatRef, updates, com.google.firebase.firestore.SetOptions.merge())
                }
            }.await()
            
            Log.d("ChatRepository", "sendMessage: Success")

            // 3. Trigger Notification to recipient
            if (recipientId != null) {
                val senderName = if (senderId == chat?.patientId) chat?.patientName else chat?.doctorName
                val preview = when (finalMessage.messageType) {
                    ChatMessageType.IMAGE -> "sent an image"
                    ChatMessageType.REPORT -> "sent a report"
                    else -> finalMessage.messageText
                }
                
                notificationRepository?.sendNotification(
                    recipientId = recipientId,
                    title = senderName ?: "New Message",
                    message = preview,
                    data = mapOf(
                        "target_screen" to "chat",
                        "chatId" to chatId,
                        "senderName" to (senderName ?: "")
                    )
                )
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e("ChatRepository", "sendMessage failed", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(context: Context, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Read image into bytes and encode as Base64
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(Exception("Cannot open image. Please try another photo."))
                val imageBytes = inputStream.use { it.readBytes() }
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                // POST to ImgBB API
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
                    Log.e("ChatRepository", "ImgBB HTTP $responseCode: $errBody")
                    return@withContext Result.failure(Exception("Upload failed (HTTP $responseCode). Check API key."))
                }

                val responseBody = connection.inputStream.bufferedReader().readText()
                Log.d("ChatRepository", "ImgBB response: $responseBody")
                val json = JSONObject(responseBody)
                val imageUrl = json.getJSONObject("data").getString("url")
                Log.d("ChatRepository", "uploadImage success: $imageUrl")
                Result.success(imageUrl)
            } catch (e: Exception) {
                Log.e("ChatRepository", "uploadImage failed: ${e.message}", e)
                Result.failure(Exception("Upload failed: ${e.message}"))
            }
        }
    }

    override fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    override fun getDoctorChats(doctorId: String): Flow<List<Chat>> = callbackFlow {
        val subscription = chatsCollection
            .whereEqualTo("doctorId", doctorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java)
                    ?.sortedByDescending { it.lastMessageTime }
                    ?: emptyList()
                trySend(chats)
            }
        awaitClose { subscription.remove() }
    }

    override fun getPatientChats(patientId: String): Flow<List<Chat>> = callbackFlow {
        val subscription = chatsCollection
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java)
                    ?.sortedByDescending { it.lastMessageTime }
                    ?: emptyList()
                trySend(chats)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun markChatAsRead(chatId: String, userId: String): Result<Boolean> {
        return try {
            Log.d("ChatRepository", "markChatAsRead: chatId=$chatId, userId=$userId")
            // Try specific update first (efficient, leaves other fields alone)
            chatsCollection.document(chatId)
                .update("unreadCounts.$userId", 0)
                .await()
            Result.success(true)
        } catch (e: Exception) {
             Log.w("ChatRepository", "markChatAsRead update failed, retrying with set-merge", e)
             // Fallback: If unreadCounts field is missing, update() fails.
             // Use set() with merge to create it.
             try {
                 val updates = mapOf("unreadCounts" to mapOf(userId to 0))
                 chatsCollection.document(chatId)
                    .set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                 Result.success(true)
             } catch (e2: Exception) {
                 Log.e("ChatRepository", "markChatAsRead set-merge failed", e2)
                 Result.failure(e2)
             }
        }
    }

    override fun getTotalUnreadCount(userId: String): Flow<Int> = callbackFlow {
        Log.d("ChatRepository", "getTotalUnreadCount: Starting observation for $userId")
        val subscription = chatsCollection
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "getTotalUnreadCount: Listen failed", error)
                    // Don't close signal, maybe retry? But standard callbackFlow behavior implies closing.
                    // close(error) 
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java) ?: emptyList()
                var totalUnread = 0
                for (chat in chats) {
                    val rawCount = chat.unreadCounts[userId]
                    val count = rawCount?.toInt() ?: 0
                    totalUnread += count
                    Log.d("ChatRepository", "Chat ${chat.chatId}: rawUnread=$rawCount, count=$count, map=${chat.unreadCounts}")
                }
                Log.d("ChatRepository", "Total unread for $userId: $totalUnread")
                trySend(totalUnread)
            }
        awaitClose { 
            Log.d("ChatRepository", "getTotalUnreadCount: Stopping observation")
            subscription.remove() 
        }
    }
}
