package com.skinsense.ai.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AppointmentRepository {
    suspend fun bookAppointment(appointment: Appointment): Result<Boolean>
    fun getPatientAppointments(userId: String): Flow<List<Appointment>>
    fun getDoctorAppointments(doctorId: String): Flow<List<Appointment>>
    fun getDoctorAppointmentsByDate(doctorId: String, date: String): Flow<List<Appointment>>
    suspend fun cancelAppointment(appointmentId: String): Result<Boolean>
    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Boolean>
}

class FirestoreAppointmentRepository : AppointmentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val appointmentsCollection = firestore.collection("appointments")

    override suspend fun bookAppointment(appointment: Appointment): Result<Boolean> {
        return try {
            val docRef = appointmentsCollection.document()
            val newAppointment = appointment.copy(appointmentId = docRef.id)
            docRef.set(newAppointment).await()
            Log.d("AppointmentRepository", "Appointment booked: ${docRef.id}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "bookAppointment failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getPatientAppointments(userId: String): Flow<List<Appointment>> = callbackFlow {
        val sub = appointmentsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.toObjects(Appointment::class.java)
                    ?.sortedByDescending { it.appointmentDate }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    override fun getDoctorAppointments(doctorId: String): Flow<List<Appointment>> = callbackFlow {
        val sub = appointmentsCollection
            .whereEqualTo("doctorId", doctorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.toObjects(Appointment::class.java)
                    ?.sortedByDescending { it.appointmentDate }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    override fun getDoctorAppointmentsByDate(doctorId: String, date: String): Flow<List<Appointment>> = callbackFlow {
        val sub = appointmentsCollection
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("appointmentDate", date)
            .whereIn("status", listOf("confirmed", "completed")) // Only block slots for active appointments
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.toObjects(Appointment::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { sub.remove() }
    }

    override suspend fun cancelAppointment(appointmentId: String): Result<Boolean> {
        return updateAppointmentStatus(appointmentId, "cancelled")
    }

    override suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Boolean> {
        return try {
            appointmentsCollection.document(appointmentId)
                .update("status", status).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "updateAppointmentStatus failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
