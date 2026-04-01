package com.skinsense.ai.data

data class Appointment(
    val appointmentId: String = "",
    val userId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val predictedDisease: String = "",
    val appointmentDate: String = "",   // e.g. "2025-03-10"
    val appointmentTime: String = "",   // e.g. "04:30 PM"
    val consultationFee: Int = 0,
    val status: String = "pending"      // "pending" | "confirmed" | "completed"
)
