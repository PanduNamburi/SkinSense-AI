package com.skinsense.ai.data

enum class ChatMessageType {
    TEXT, IMAGE, REPORT
}

data class ReportAttachment(
    val diseaseName: String = "",
    val category: String = "",
    val severityLevel: String = "",
    val severityPercentage: Float = 0f,
    val confidence: Float = 0f,
    val date: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "", 
    val senderId: String = "",
    val senderRole: String = "", 
    val messageText: String = "",
    val messageType: ChatMessageType = ChatMessageType.TEXT,
    val attachmentUrl: String? = null,
    val reportAttachment: ReportAttachment? = null,
    val timestamp: Long = System.currentTimeMillis()
)
