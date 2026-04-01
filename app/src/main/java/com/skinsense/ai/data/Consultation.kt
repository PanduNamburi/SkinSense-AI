package com.skinsense.ai.data

enum class ConsultationStatus {
    PENDING,
    ACCEPTED,
    COMPLETED,
    REJECTED
}

data class Consultation(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String? = null,
    val status: ConsultationStatus = ConsultationStatus.PENDING,
    val imageUri: String = "",
    val diseaseName: String = "",
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val patientName: String = "",
    val doctorName: String? = null,
    val paymentStatus: String = "pending", // "pending", "paid"
    val paymentId: String? = null
)
