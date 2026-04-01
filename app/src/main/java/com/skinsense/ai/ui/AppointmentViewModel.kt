package com.skinsense.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.skinsense.ai.data.Appointment
import com.skinsense.ai.data.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.skinsense.ai.data.*
import com.skinsense.ai.utils.PaymentManager
import com.skinsense.ai.utils.PaymentResult
import org.json.JSONObject
import android.app.Activity
import com.skinsense.ai.BuildConfig

sealed class BookingState {
    object Idle : BookingState()
    object Loading : BookingState()
    object Success : BookingState()
    data class Error(val message: String) : BookingState()
}

class AppointmentViewModel(
    private val repository: AppointmentRepository,
    private val paymentRepository: PaymentRepository,
    private val consultationRepository: ConsultationRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _bookedTimeSlots = MutableStateFlow<List<String>>(emptyList())
    val bookedTimeSlots: StateFlow<List<String>> = _bookedTimeSlots.asStateFlow()

    init {
        viewModelScope.launch {
            PaymentManager.paymentResult.collect { result ->
                handlePaymentResult(result)
            }
        }
    }

    private var pendingBookingInfo: JSONObject? = null

    fun initiatePayment(
        activity: Activity,
        doctorId: String,
        doctorName: String,
        consultationFee: Int,
        userEmail: String,
        userPhone: String,
        date: String,
        time: String,
        predictedDisease: String
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        try {
            val orderId = "ORDER_${System.currentTimeMillis()}"
            val amountInPaise = (consultationFee * 100).toString()

            // Store info for later
            pendingBookingInfo = JSONObject().apply {
                put("orderId", orderId)
                put("doctorId", doctorId)
                put("doctorName", doctorName)
                put("amount", consultationFee)
                put("userId", userId)
                put("date", date)
                put("time", time)
                put("predictedDisease", predictedDisease)
            }

            PaymentManager.startRazorpayPayment(
                activity = activity,
                amountInPaise = amountInPaise,
                userEmail = userEmail,
                userPhone = userPhone,
                doctorName = doctorName,
                orderId = orderId
            )
        } catch (e: Exception) {
            _bookingState.value = BookingState.Error("Payment initiation failed: ${e.message}")
        }
    }

    private fun handlePaymentResult(result: PaymentResult) {
        when (result) {
            is PaymentResult.Success -> {
                val info = pendingBookingInfo ?: return
                processSuccessfulPayment(result.orderId, info)
            }
            is PaymentResult.Error -> {
                _bookingState.value = BookingState.Error(result.message ?: "Payment failed")
            }
        }
    }

    private fun processSuccessfulPayment(paymentId: String, info: JSONObject) {
        val userId = info.getString("userId")
        val doctorId = info.getString("doctorId")
        val doctorName = info.getString("doctorName")
        val amount = info.getInt("amount")
        val date = info.optString("date", "")
        val time = info.optString("time", "")
        val predictedDisease = info.optString("predictedDisease", "")

        _bookingState.value = BookingState.Loading

        viewModelScope.launch {
            // 1. Record Payment
            val payment = Payment(
                paymentId = paymentId,
                userId = userId,
                doctorId = doctorId,
                amount = amount,
                status = "success"
            )
            paymentRepository.recordPayment(payment)

            // 2. Create Consultation
            val consultation = Consultation(
                patientId = userId,
                doctorId = doctorId,
                doctorName = doctorName,
                diseaseName = predictedDisease,
                status = ConsultationStatus.PENDING,
                paymentStatus = "paid",
                paymentId = paymentId
            )
            val consultResult = consultationRepository.requestConsultation(consultation)

            // 3. Create Appointment
            val appointment = Appointment(
                userId = userId,
                doctorId = doctorId,
                doctorName = doctorName,
                predictedDisease = predictedDisease,
                appointmentDate = date,
                appointmentTime = time,
                consultationFee = amount,
                status = "confirmed"
            )
            val appointResult = repository.bookAppointment(appointment)


            if (consultResult.isSuccess && appointResult.isSuccess) {
                _bookingState.value = BookingState.Success
                
                // 4. Trigger Notification to Doctor
                notificationRepository.sendNotification(
                    recipientId = doctorId,
                    title = "New Appointment Booked",
                    message = "A patient has booked a consultation with you for $date at $time.",
                    data = mapOf("target_screen" to "doctor_consultations")
                )

                // 5. Trigger Notification to Patient
                notificationRepository.sendNotification(
                    recipientId = userId,
                    title = "Appointment Confirmed",
                    message = "Your appointment with Dr. $doctorName is confirmed for $date at $time.",
                    data = mapOf("target_screen" to "my_appointments")
                )
            } else {
                _bookingState.value = BookingState.Error("Payment verified, but failed to create full consultation/appointment record.")
            }
            pendingBookingInfo = null
        }
    }

    fun bookAppointment(
        doctorId: String,
        doctorName: String,
        date: String,
        time: String,
        predictedDisease: String = "",
        consultationFee: Int = 0
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _bookingState.value = BookingState.Loading

        viewModelScope.launch {
            val appointment = Appointment(
                userId = userId,
                doctorId = doctorId,
                doctorName = doctorName,
                predictedDisease = predictedDisease,
                appointmentDate = date,
                appointmentTime = time,
                consultationFee = consultationFee,
                status = "pending"
            )
            val result = repository.bookAppointment(appointment)
            _bookingState.value = if (result.isSuccess) {
                BookingState.Success
            } else {
                BookingState.Error(result.exceptionOrNull()?.message ?: "Booking failed")
            }
        }
    }

    fun loadPatientAppointments() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getPatientAppointments(userId).collect {
                _appointments.value = it
            }
        }
    }

    fun resetBookingState() {
        _bookingState.value = BookingState.Idle
    }

    fun loadBookedSlots(doctorId: String, date: String) {
        viewModelScope.launch {
            repository.getDoctorAppointmentsByDate(doctorId, date).collect { apps ->
                val slots = apps.map { it.appointmentTime }
                _bookedTimeSlots.value = slots
            }
        }
    }

    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            repository.cancelAppointment(appointmentId)
        }
    }
}

class AppointmentViewModelFactory(
    private val repository: AppointmentRepository,
    private val paymentRepository: PaymentRepository,
    private val consultationRepository: ConsultationRepository,
    private val notificationRepository: NotificationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppointmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppointmentViewModel(repository, paymentRepository, consultationRepository, notificationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
