package com.skinsense.ai.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface PaymentRepository {
    suspend fun recordPayment(payment: Payment): Result<Boolean>
}

class FirestorePaymentRepository : PaymentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val paymentsCollection = firestore.collection("payments")

    override suspend fun recordPayment(payment: Payment): Result<Boolean> {
        return try {
            val docRef = if (payment.paymentId.isEmpty()) paymentsCollection.document() else paymentsCollection.document(payment.paymentId)
            val finalPayment = if (payment.paymentId.isEmpty()) payment.copy(paymentId = docRef.id) else payment
            docRef.set(finalPayment).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SkinSense", "recordPayment failed: ${e.message}")
            Result.failure(e)
        }
    }
}
