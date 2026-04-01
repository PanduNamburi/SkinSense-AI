package com.skinsense.ai.data

enum class ConnectionStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class ConnectionRequest(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val status: ConnectionStatus = ConnectionStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)
