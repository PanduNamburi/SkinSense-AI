package com.skinsense.ai.data

data class Chat(
    val chatId: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val patientName: String = "",
    val doctorName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val participants: List<String> = emptyList(), // For easier querying
    val unreadCounts: Map<String, Long> = emptyMap() // Map of userId -> unreadCount
)
