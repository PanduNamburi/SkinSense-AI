package com.skinsense.ai.data

import android.net.Uri
import com.skinsense.ai.ui.compose.SeverityLevel

/**
 * Analysis Result Data
 */
data class AnalysisResult(
    val diseaseName: String,
    val category: String,
    val confidence: Float,
    val severityLevel: SeverityLevel = SeverityLevel.MILD,
    val severityPercentage: Float = 0f,
    val description: String,
    val symptoms: List<String>,
    val causes: List<String>,
    val recommendedActions: List<String>,
    val imageUri: Uri? = null,
    val imageBase64: String? = null
)
