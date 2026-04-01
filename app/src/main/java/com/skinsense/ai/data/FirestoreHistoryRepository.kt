package com.skinsense.ai.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.skinsense.ai.ui.compose.HistoryItem
import com.skinsense.ai.ui.compose.SeverityLevel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreHistoryRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun saveHistory(userId: String, result: AnalysisResult): Result<Boolean> {
        return try {
            val historyCollection = firestore.collection("users").document(userId).collection("scan_history")
            
            val data = mutableMapOf(
                "diseaseName" to result.diseaseName,
                "category" to result.category,
                "severityLevel" to result.severityLevel.name,
                "severityPercentage" to result.severityPercentage,
                "confidence" to result.confidence,
                "timestamp" to System.currentTimeMillis(),
                "imageUri" to (result.imageUri?.toString() ?: ""),
                "imageBase64" to (result.imageBase64 ?: "")
            )

            historyCollection.add(data).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("FirestoreHistoryRepo", "Error saving history: ${e.message}")
            Result.failure(e)
        }
    }

    fun getHistory(userId: String): Flow<List<HistoryItem>> = callbackFlow {
        val collection = firestore.collection("users").document(userId).collection("scan_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        val diseaseName = doc.getString("diseaseName") ?: "Unknown"
                        val category = doc.getString("category") ?: "Uncategorized"
                        val severityString = doc.getString("severityLevel") ?: "MILD"
                        val severityLevel = try { SeverityLevel.valueOf(severityString) } catch (e: Exception) { SeverityLevel.MILD }
                        val severityPercentage = doc.getDouble("severityPercentage")?.toFloat() ?: 0f
                        val confidence = doc.getDouble("confidence")?.toFloat() ?: 0f
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val imageUri = doc.getString("imageUri")
                        val imageBase64 = doc.getString("imageBase64")
                        
                        HistoryItem(
                            id = doc.id,
                            diseaseName = diseaseName,
                            category = category,
                            severityLevel = severityLevel,
                            severityPercentage = severityPercentage,
                            confidence = confidence,
                            date = Date(timestamp),
                            imageUri = imageUri,
                            imageBase64 = imageBase64
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(items)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getHistoryItem(userId: String, historyId: String): HistoryItem? {
        return try {
             val doc = firestore.collection("users").document(userId).collection("scan_history")
                 .document(historyId).get().await()
             
             if (doc.exists()) {
                 val diseaseName = doc.getString("diseaseName") ?: "Unknown"
                 val category = doc.getString("category") ?: "Uncategorized"
                 val severityString = doc.getString("severityLevel") ?: "MILD"
                 val severityLevel = try { SeverityLevel.valueOf(severityString) } catch (e: Exception) { SeverityLevel.MILD }
                 val severityPercentage = doc.getDouble("severityPercentage")?.toFloat() ?: 0f
                 val confidence = doc.getDouble("confidence")?.toFloat() ?: 0f
                 val timestamp = doc.getLong("timestamp") ?: 0L
                 val imageUri = doc.getString("imageUri")
                 val imageBase64 = doc.getString("imageBase64")

                HistoryItem(
                    id = doc.id,
                    diseaseName = diseaseName,
                    category = category,
                    severityLevel = severityLevel,
                    severityPercentage = severityPercentage,
                    confidence = confidence,
                    date = Date(timestamp),
                    imageUri = imageUri,
                    imageBase64 = imageBase64
                )
             } else {
                 null
             }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteHistory(userId: String, historyId: String): Result<Boolean> {
        return try {
             firestore.collection("users").document(userId).collection("scan_history")
                 .document(historyId).delete().await()
             Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllHistory(userId: String): Result<Boolean> {
        return try {
            val collection = firestore.collection("users").document(userId).collection("scan_history")
            val snapshot = collection.get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("FirestoreHistoryRepo", "Error clearing history: ${e.message}")
            Result.failure(e)
        }
    }
}
