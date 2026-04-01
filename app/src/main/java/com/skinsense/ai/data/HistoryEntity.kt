package com.skinsense.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // Firebase UID
    val imagePath: String,
    val topDiagnosis: String,
    val confidence: Float,
    val timestamp: Long
)
