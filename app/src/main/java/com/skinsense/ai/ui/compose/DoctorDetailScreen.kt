package com.skinsense.ai.ui.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.data.AnalysisResult
import com.skinsense.ai.data.User
import com.skinsense.ai.ui.AppointmentViewModel
import com.skinsense.ai.ui.BookingState
import com.skinsense.ai.ui.FindDoctorViewModel
import com.skinsense.ai.ui.theme.AccentBlue
import com.skinsense.ai.ui.theme.PrimaryDark
import com.skinsense.ai.ui.theme.TextPrimary
import com.skinsense.ai.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import android.app.Activity
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileDetailScreen(
    doctorId: String,
    analysisResult: AnalysisResult?,
    viewModel: FindDoctorViewModel,
    appointmentViewModel: AppointmentViewModel,
    onBack: () -> Unit,
    onConsultationInitiated: () -> Unit,
    onChatClick: (String) -> Unit
) {
    val doctors by viewModel.doctors.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val doctor = doctors.find { it.uid == doctorId }
    
    val bookingState by appointmentViewModel.bookingState.collectAsState()
    val bookedTimeSlots by appointmentViewModel.bookedTimeSlots.collectAsState()
    val context = LocalContext.current

    // Calendar state
    val today = remember { LocalDate.now() }
    val next14Days = remember { (0..13).map { today.plusDays(it.toLong()) } }
    var selectedDate by remember { mutableStateOf(today) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        appointmentViewModel.resetBookingState()
        if (doctor == null) {
            viewModel.fetchDoctorById(doctorId)
        }
    }

    LaunchedEffect(selectedDate, doctor) {
        if (doctor != null) {
            appointmentViewModel.loadBookedSlots(
                doctorId = doctor.uid,
                date = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            )
        }
    }

    LaunchedEffect(bookingState) {
        if (bookingState is BookingState.Success) {
            Toast.makeText(context, "Appointment Booked Successfully!", Toast.LENGTH_LONG).show()
            appointmentViewModel.resetBookingState()
            onBack()
        } else if (bookingState is BookingState.Error) {
            Toast.makeText(context, (bookingState as BookingState.Error).message, Toast.LENGTH_LONG).show()
            appointmentViewModel.resetBookingState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctor Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (doctor != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = Color.White
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Consultation Fee", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("₹${doctor.consultationFee}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AccentBlue)
                            }
                            Button(
                                onClick = {
                                    if (selectedTime == null) {
                                        Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    val activity = context as? Activity ?: return@Button
                                    
                                    appointmentViewModel.initiatePayment(
                                        activity = activity,
                                        doctorId = doctor.uid,
                                        doctorName = doctor.displayName,
                                        consultationFee = doctor.consultationFee,
                                        userEmail = currentUser?.email ?: "",
                                        userPhone = currentUser?.phoneNumber ?: "",
                                        date = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                        time = selectedTime!!,
                                        predictedDisease = analysisResult?.diseaseName ?: ""
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                enabled = bookingState !is BookingState.Loading
                            ) {
                                if (bookingState is BookingState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                } else {
                                    Text("Pay & Book Consultation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        if (isLoading) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (doctor == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Doctor not found", color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Doctor Card matching reference UI
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(doctor.displayName.take(1).uppercase(), color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(doctor.specialization ?: "Dermatologist", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Dr. ${doctor.displayName}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentBlue,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rating ${doctor.rating} (${doctor.reviewCount} Reviews)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(horizontalArrangement = Arrangement.Center) {
                            StatBox(label = "Experience", value = "${doctor.experienceYears ?: 5} Yrs")
                            Spacer(modifier = Modifier.width(32.dp))
                            StatBox(label = "Availability", value = if(doctor.isAvailableForBooking) "Today" else "Soon")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Calendar Section
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(next14Days) { date ->
                            val isSelected = date == selectedDate
                            val textColor = if (isSelected) Color.White else TextPrimary
                            val bgColor = if (isSelected) AccentBlue else Color.White
                            
                            Surface(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(72.dp)
                                    .clickable { 
                                        selectedDate = date 
                                        selectedTime = null // reset time when day changes
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = bgColor,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Time Slots Section
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val availableSlots = doctor.availableSlots.filter { it !in bookedTimeSlots }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available Time Slots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${availableSlots.size} Slots", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (availableSlots.isEmpty()) {
                            Text("No slots available for this date.", color = TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            availableSlots.forEach { time ->
                                val isSelected = time == selectedTime
                                Surface(
                                    modifier = Modifier.clickable { selectedTime = time },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AccentBlue else Color.White,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                                ) {
                                    Text(
                                        time,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (analysisResult != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Booking for Predicted Condition:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(analysisResult.diseaseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom bar
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
