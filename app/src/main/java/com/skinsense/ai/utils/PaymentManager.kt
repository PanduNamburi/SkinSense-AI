package com.skinsense.ai.utils

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.paytm.pgsdk.PaytmOrder
import com.paytm.pgsdk.PaytmPaymentTransactionCallback
import com.paytm.pgsdk.TransactionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class PaymentResult {
    data class Success(val orderId: String, val transactionResponse: String?) : PaymentResult()
    data class Error(val message: String?) : PaymentResult()
}

object PaymentManager {
    private val _paymentResult = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 1)
    val paymentResult = _paymentResult.asSharedFlow()

    fun startRazorpayPayment(
        activity: Activity,
        amountInPaise: String, // Amount string must be in paise (e.g., "50000" for ₹500)
        userEmail: String,
        userPhone: String,
        doctorName: String,
        orderId: String
    ) {
        val checkout = com.razorpay.Checkout()
        // Explicitly setting the API key helps bypass some test-mode KYC prompts
        checkout.setKeyID(com.skinsense.ai.BuildConfig.RAZORPAY_API_KEY)

        try {
            val options = org.json.JSONObject()
            options.put("name", "SkinSense AI")
            options.put("description", "Consultation with $doctorName")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg") // Replace with actual logo URL
            options.put("theme.color", "#1565C0")
            options.put("currency", "INR")
            options.put("amount", amountInPaise)
            // options.put("order_id", orderId) // If using server-side Orders API

            val retryObj = org.json.JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            val prefill = org.json.JSONObject()
            prefill.put("email", userEmail)
            prefill.put("contact", userPhone)
            options.put("prefill", prefill)

            checkout.open(activity, options)

        } catch (e: Exception) {
            Log.e("SkinSense", "Error in starting Razorpay Checkout", e)
            _paymentResult.tryEmit(PaymentResult.Error(e.message))
        }
    }

    fun handleRazorpaySuccess(paymentId: String?, paymentData: com.razorpay.PaymentData?) {
        // Retrieve internal order ID or external ID from paymentData if needed
        val externalOrderId = paymentData?.orderId ?: "ORDER_${System.currentTimeMillis()}"
        _paymentResult.tryEmit(PaymentResult.Success(externalOrderId, paymentId))
    }

    fun handleRazorpayError(errorCode: Int, response: String?) {
        _paymentResult.tryEmit(PaymentResult.Error(response ?: "Payment failed with error code $errorCode"))
    }
}
