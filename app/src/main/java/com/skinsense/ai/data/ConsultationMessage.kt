package com.skinsense.ai.data

data class ConsultationMessage(
    val id: String = "",
    val consultationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "REPORT"
    val attachmentUrl: String? = null,
    val reportAttachment: ReportAttachment? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystemMessage: Boolean = false
)
