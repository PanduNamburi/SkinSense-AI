package com.skinsense.ai.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.data.User
import com.skinsense.ai.ui.AdminDashboardViewModel
import com.skinsense.ai.ui.theme.AccentBlue
import com.skinsense.ai.ui.theme.PrimaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onBack: () -> Unit
) {
    val pendingDoctors by viewModel.pendingDoctors.collectAsState()
    val allDoctors by viewModel.allDoctors.collectAsState()
    val allPatients by viewModel.allPatients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMsg by viewModel.successMessage.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending (${pendingDoctors.size})", "Doctors", "Patients")

    // Show snackbar messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error, successMsg) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        successMsg?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.loadAllData() },
                containerColor = AccentBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Surface(color = AccentBlue, modifier = Modifier.fillMaxSize()) {}
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                when (selectedTab) {
                    0 -> PendingDoctorsTab(
                        doctors = pendingDoctors,
                        onApprove = viewModel::approveDoctor,
                        onReject = viewModel::rejectDoctor
                    )
                    1 -> AllDoctorsTab(
                        doctors = allDoctors,
                        onActivate = { uid -> viewModel.setDoctorActive(uid, true) },
                        onDeactivate = { uid -> viewModel.setDoctorActive(uid, false) }
                    )
                    2 -> AllPatientsTab(
                        patients = allPatients,
                        onActivate = { uid -> viewModel.setPatientActive(uid, true) },
                        onDeactivate = { uid -> viewModel.setPatientActive(uid, false) }
                    )
                }
            }
        }
    }
}

@Composable
fun PendingDoctorsTab(
    doctors: List<User>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (doctors.isEmpty()) {
        EmptyState(icon = "✅", message = "No pending verifications!")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(doctors) { doctor ->
                AdminDoctorCard(
                    doctor = doctor,
                    showApproveReject = true,
                    onApprove = { onApprove(doctor.uid) },
                    onReject = { onReject(doctor.uid) }
                )
            }
        }
    }
}

@Composable
fun AllDoctorsTab(
    doctors: List<User>,
    onActivate: (String) -> Unit,
    onDeactivate: (String) -> Unit
) {
    if (doctors.isEmpty()) {
        EmptyState(icon = "🩺", message = "No doctors registered yet.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(doctors) { doctor ->
                AdminDoctorCard(
                    doctor = doctor,
                    showApproveReject = false,
                    onActivate = if (doctor.isActive) null else { { onActivate(doctor.uid) } },
                    onDeactivate = if (doctor.isActive) { { onDeactivate(doctor.uid) } } else null
                )
            }
        }
    }
}

@Composable
fun AllPatientsTab(
    patients: List<User>,
    onActivate: (String) -> Unit,
    onDeactivate: (String) -> Unit
) {
    if (patients.isEmpty()) {
        EmptyState(icon = "👤", message = "No patients registered yet.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(patients) { patient ->
                AdminUserCard(
                    user = patient,
                    onActivate = if (patient.isActive) null else { { onActivate(patient.uid) } },
                    onDeactivate = if (patient.isActive) { { onDeactivate(patient.uid) } } else null
                )
            }
        }
    }
}

@Composable
fun AdminDoctorCard(
    doctor: User,
    showApproveReject: Boolean,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onActivate: (() -> Unit)? = null,
    onDeactivate: (() -> Unit)? = null
) {
    val statusColor = when (doctor.status) {
        "approved" -> Color(0xFF059669)
        "rejected" -> Color(0xFFDC2626)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFF059669).copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.LocalHospital, null, tint = Color(0xFF059669), modifier = Modifier.padding(10.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(doctor.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(doctor.specialization ?: "General", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.1f)) {
                    Text(
                        doctor.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                        color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Hospital: ${doctor.hospitalName ?: "N/A"}", fontSize = 13.sp, color = Color.Gray)
            Text("License: ${doctor.licenseNumber ?: "N/A"}", fontSize = 13.sp, color = Color.Gray)
            Text("Exp: ${doctor.experienceYears ?: 0} yrs • ${doctor.qualifications ?: "N/A"}", fontSize = 13.sp, color = Color.Gray)
            if (!doctor.isActive) {
                Text("⚠️ Account Deactivated", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (showApproveReject) {
                    OutlinedButton(
                        onClick = { onReject?.invoke() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApprove?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                } else {
                    onDeactivate?.let {
                        OutlinedButton(onClick = it, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))) {
                            Text("Deactivate")
                        }
                    }
                    onActivate?.let {
                        Button(onClick = it, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))) {
                            Text("Activate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: User,
    onActivate: (() -> Unit)? = null,
    onDeactivate: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = AccentBlue.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Person, null, tint = AccentBlue, modifier = Modifier.padding(10.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(user.email, fontSize = 12.sp, color = Color.Gray)
                }
                if (!user.isActive) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFDC2626).copy(alpha = 0.1f)) {
                        Text("Inactive", color = Color(0xFFDC2626), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                } else {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF059669).copy(alpha = 0.1f)) {
                        Text("Active", color = Color(0xFF059669), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            user.phone?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("📞 $it", fontSize = 13.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                onDeactivate?.let {
                    OutlinedButton(onClick = it, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))) {
                        Text("Deactivate")
                    }
                }
                onActivate?.let {
                    Button(onClick = it, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))) {
                        Text("Activate")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(icon: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = Color.Gray, fontSize = 16.sp)
        }
    }
}
