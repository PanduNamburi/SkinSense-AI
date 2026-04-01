package com.skinsense.ai.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.data.Appointment
import com.skinsense.ai.ui.AppointmentViewModel
import com.skinsense.ai.ui.theme.AccentBlue
import com.skinsense.ai.ui.theme.MedicalSecondary
import com.skinsense.ai.ui.theme.TextPrimary
import com.skinsense.ai.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(
    appointmentViewModel: AppointmentViewModel,
    onBack: () -> Unit
) {
    val appointments by appointmentViewModel.appointments.collectAsState()

    LaunchedEffect(Unit) {
        appointmentViewModel.loadPatientAppointments()
    }

    val upcoming = appointments.filter { it.status != "completed" && it.status != "cancelled" }
    val past = appointments.filter { it.status == "completed" || it.status == "cancelled" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Appointments", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        if (appointments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No appointments yet", color = TextSecondary, fontSize = 16.sp)
                    Text("Book a consultation with a doctor", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (upcoming.isNotEmpty()) {
                    item {
                        Text("Upcoming", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(upcoming) { appt ->
                        AppointmentCard(appointment = appt, onCancel = { appointmentViewModel.cancelAppointment(appt.appointmentId) })
                    }
                }
                if (past.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Past", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(past) { appt ->
                        AppointmentCard(appointment = appt, onCancel = null)
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment, onCancel: (() -> Unit)?) {
    val statusColor = when (appointment.status) {
        "confirmed" -> Color(0xFF2E7D32)
        "completed" -> Color(0xFF1565C0)
        "cancelled"  -> Color(0xFFC62828)
        else         -> Color(0xFFE65100) // pending = orange
    }
    val statusLabel = appointment.status.replaceFirstChar { it.uppercase() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(AccentBlue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(appointment.doctorName.take(1).uppercase(), color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dr. ${appointment.doctorName}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    if (appointment.predictedDisease.isNotBlank()) {
                        Text(appointment.predictedDisease, color = TextSecondary, fontSize = 12.sp)
                    }
                }
                Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.12f)) {
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
            Row {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(appointment.appointmentDate, color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Schedule, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(appointment.appointmentTime, color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("₹${appointment.consultationFee}", color = MedicalSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (onCancel != null && appointment.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Text("Cancel Appointment", fontSize = 12.sp)
                }
            }
        }
    }
}
