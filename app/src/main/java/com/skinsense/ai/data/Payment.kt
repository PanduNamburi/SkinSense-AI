package com.skinsense.ai.data

data class Payment(
    val paymentId: String = "",
    val userId: String = "",
    val doctorId: String = "",
    val consultationId: String = "",
    val amount: Int = 0,
    val status: String = "", // "success", "failed"
    val paymentGateway: String = "razorpay",
    val timestamp: Long = System.currentTimeMillis()
)
