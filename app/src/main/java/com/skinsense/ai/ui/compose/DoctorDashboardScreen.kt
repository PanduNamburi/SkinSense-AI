package com.skinsense.ai.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skinsense.ai.data.Appointment
import com.skinsense.ai.data.Consultation
import com.skinsense.ai.ui.DoctorDashboardViewModel
import com.skinsense.ai.ui.theme.AccentBlue
import com.skinsense.ai.ui.theme.PrimaryDark
import com.skinsense.ai.ui.theme.TextPrimary
import com.skinsense.ai.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(
    viewModel: DoctorDashboardViewModel,
    onLogout: () -> Unit,
    onNavigateToChat: (String) -> Unit, // For Consultation Chat (Legacy/Specific)
    onChatSelected: (String, String) -> Unit, // For General Chat (New)
    onNavigateToPatientDetail: (String) -> Unit // New: Navigate to patient details
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Requests", "My Patients", "Chats", "Profile")
    val icons = listOf(Icons.Default.PendingActions, Icons.Default.People, Icons.Default.Chat, Icons.Default.Person)

    val totalUnreadCount by viewModel.totalUnreadCount.collectAsState()

    Scaffold(
        // topBar removed to use global header
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label) },
                        icon = {
                            if (index == 2) { // Chats tab
                                BadgedBox(
                                    badge = {
                                        if (totalUnreadCount > 0) {
                                            Badge { Text(text = totalUnreadCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(icons[index], contentDescription = label)
                                }
                            } else {
                                Icon(icons[index], contentDescription = label)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    val requests by viewModel.pendingRequests.collectAsState()
                    val connectionRequests by viewModel.connectionRequests.collectAsState()
                    val appointmentRequests by viewModel.pendingAppointments.collectAsState()
                    ConsultationRequestsTab(
                        requests = requests, 
                        connectionRequests = connectionRequests,
                        appointmentRequests = appointmentRequests,
                        onAcceptConsultation = { viewModel.acceptRequest(it) },
                        onAcceptConnection = { viewModel.approveConnectionRequest(it) },
                        onRejectConnection = { viewModel.rejectConnectionRequest(it) },
                        onAcceptAppointment = { viewModel.acceptAppointment(it.appointmentId) },
                        onRejectAppointment = { viewModel.declineAppointment(it.appointmentId) }
                    )
                }
                1 -> {
                    val consultations by viewModel.myConsultations.collectAsState()
                    val connectedPatients by viewModel.connectedPatients.collectAsState()
                    val upcomingAppointments by viewModel.upcomingAppointments.collectAsState()
                    MyPatientsTab(
                        consultations = consultations, 
                        connectedPatients = connectedPatients,
                        upcomingAppointments = upcomingAppointments,
                        onChatClick = onNavigateToChat,
                        onPatientClick = { patient -> 
                            onNavigateToPatientDetail(patient.uid)
                        }
                    )
                }
                2 -> {
                    val chats by viewModel.chats.collectAsState()
                    ChatListContent(
                        chats = chats,
                        currentUserRole = com.skinsense.ai.data.UserRole.DOCTOR,
                        onChatClick = { chat ->
                             // Navigate to generic chat
                             val otherName = if (chat.patientName.isNotBlank()) chat.patientName else "Patient"
                             onChatSelected(chat.chatId, otherName)
                        }
                    )
                }
                3 -> {
                    val profile by viewModel.doctorProfile.collectAsState()
                    DoctorProfileTab(profile = profile)
                }
            }
        }
    }
}

@Composable
fun ConsultationRequestsTab(
    requests: List<Consultation>,
    connectionRequests: List<com.skinsense.ai.data.ConnectionRequest>,
    appointmentRequests: List<Appointment>,
    onAcceptConsultation: (Consultation) -> Unit,
    onAcceptConnection: (com.skinsense.ai.data.ConnectionRequest) -> Unit,
    onRejectConnection: (com.skinsense.ai.data.ConnectionRequest) -> Unit,
    onAcceptAppointment: (Appointment) -> Unit,
    onRejectAppointment: (Appointment) -> Unit
) {
    if (requests.isEmpty() && connectionRequests.isEmpty() && appointmentRequests.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Assignment,
            message = "No pending requests found"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (connectionRequests.isNotEmpty()) {
                item {
                    Text(
                        "Connection Requests",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(connectionRequests) { request ->
                    ConnectionRequestCard(
                        request = request, 
                        onAccept = { onAcceptConnection(request) },
                        onReject = { onRejectConnection(request) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (requests.isNotEmpty()) {
                item {
                    Text(
                        "Consultation Requests",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(requests) { request ->
                    ConsultationRequestCard(request = request, onAccept = { onAcceptConsultation(request) })
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            if (appointmentRequests.isNotEmpty()) {
                item {
                    Text(
                        "Appointment Requests",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(appointmentRequests) { appointment ->
                    AppointmentRequestCard(
                        appointment = appointment, 
                        onAccept = { onAcceptAppointment(appointment) },
                        onReject = { onRejectAppointment(appointment) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentRequestCard(
    appointment: Appointment,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = AccentBlue)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Appointment Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${appointment.appointmentDate} at ${appointment.appointmentTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    if (appointment.predictedDisease.isNotEmpty()) {
                        Text(
                            text = "Condition: ${appointment.predictedDisease}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Decline", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConnectionRequestCard(
    request: com.skinsense.ai.data.ConnectionRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        request.patientName.ifEmpty { "P" }.take(1).uppercase(),
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (request.patientName.isEmpty()) "Unknown Patient" else request.patientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wants to connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                     elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Decline", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConsultationRequestCard(
    request: Consultation,
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Patient Placeholder Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        request.patientName.ifEmpty { "U" }.take(1).uppercase(),
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (request.patientName.isEmpty()) "Unknown Patient" else request.patientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reported: ${formatDate(request.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Image Snippet
                AsyncImage(
                    model = request.imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Detected: ${request.diseaseName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryDark
                    )
                    Text(
                        text = "Confidence: ${String.format("%.1f", request.confidence * 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Accept Request", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MyPatientsTab(
    consultations: List<Consultation>, 
    connectedPatients: List<com.skinsense.ai.data.User>,
    upcomingAppointments: List<Appointment>,
    onChatClick: (String) -> Unit,
    onPatientClick: (com.skinsense.ai.data.User) -> Unit
) {
    if (consultations.isEmpty() && connectedPatients.isEmpty() && upcomingAppointments.isEmpty()) {
        EmptyState(
            icon = Icons.Default.People,
            message = "No active patients or appointments found"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (connectedPatients.isNotEmpty()) {
                item {
                    Text(
                        "Connected Patients",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(connectedPatients) { patient ->
                    ConnectedPatientCard(patient = patient, onClick = { onPatientClick(patient) })
                }
                 item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (consultations.isNotEmpty()) {
                 item {
                    Text(
                        "Active Consultations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(consultations) { consultation ->
                    ActiveConsultationCard(consultation = consultation, onChatClick = { onChatClick(consultation.id) })
                }
                 item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (upcomingAppointments.isNotEmpty()) {
                item {
                    Text(
                        "Upcoming Appointments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(upcomingAppointments) { appointment ->
                    UpcomingAppointmentCard(appointment = appointment)
                }
            }
        }
    }
}

@Composable
fun UpcomingAppointmentCard(appointment: Appointment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = AccentBlue)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Confirmed Appointment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${appointment.appointmentDate} at ${appointment.appointmentTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                if (appointment.predictedDisease.isNotEmpty()) {
                    Text(
                        text = "Condition: ${appointment.predictedDisease}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectedPatientCard(patient: com.skinsense.ai.data.User, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patient.displayName.ifEmpty { "P" }.take(1).uppercase(),
                     style = MaterialTheme.typography.titleLarge,
                     fontWeight = FontWeight.Bold,
                     color = AccentBlue
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (patient.displayName.isEmpty()) "Unknown Patient" else patient.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = patient.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Chat, contentDescription = "Open Chat", tint = AccentBlue)
            }
        }
    }
}

@Composable
fun ActiveConsultationCard(consultation: Consultation, onChatClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onChatClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = AccentBlue)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (consultation.patientName.isEmpty()) "Unknown Patient" else consultation.patientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = consultation.diseaseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                 Text(
                    text = "Active Consultation",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentBlue
                )
            }
            
            IconButton(onClick = onChatClick) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open Chat")
            }
        }
    }
}

@Composable
fun DoctorProfileTab(profile: com.skinsense.ai.data.User?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = AccentBlue.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = profile?.displayName?.take(1)?.uppercase() ?: "D",
                    style = MaterialTheme.typography.displayMedium,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Dr. ${profile?.displayName ?: "Doctor"}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = profile?.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Professional Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailRow(icon = Icons.Default.MedicalServices, label = "Specialization", value = profile?.specialization ?: "Not set")
                DetailRow(icon = Icons.Default.WorkHistory, label = "Experience", value = "${profile?.experienceYears ?: 0} Years")
                DetailRow(icon = Icons.Default.Badge, label = "License No", value = profile?.licenseNumber ?: "Not set")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Settings Items
        SettingsItem(icon = Icons.Default.Notifications, title = "Notification Preferences")
        SettingsItem(icon = Icons.Default.Lock, title = "Privacy & Security")
        SettingsItem(icon = Icons.Default.Help, title = "Support Center")
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
