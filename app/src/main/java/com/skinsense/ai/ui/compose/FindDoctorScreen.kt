package com.skinsense.ai.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import android.util.Log
import com.skinsense.ai.data.User
import com.skinsense.ai.ui.FindDoctorViewModel
import com.skinsense.ai.ui.theme.AccentBlue
import com.skinsense.ai.ui.theme.PrimaryDark
import com.skinsense.ai.ui.theme.TextPrimary
import com.skinsense.ai.ui.theme.TextSecondary
import com.skinsense.ai.ui.theme.MedicalSecondary
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDoctorScreen(
    resultJson: String? = null,
    viewModel: FindDoctorViewModel,
    onBack: () -> Unit,
    onDoctorClick: (User, String?) -> Unit,
    onMessageClick: (User) -> Unit
) {
    val doctors by viewModel.doctors.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val connectionStatuses by viewModel.connectionStatuses.collectAsState()
    val specializations by viewModel.specializations.collectAsState()
    val selectedSpec by viewModel.selectedSpecialization.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        Log.d("SkinSense", "FindDoctorScreen LaunchedEffect calling fetchDoctors")
        viewModel.fetchDoctors()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.find_doctor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.find_doctor_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = AccentBlue
                )
            )

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                specializations.forEach { spec ->
                    FilterChip(
                        selected = selectedSpec == spec,
                        onClick = { viewModel.selectSpecialization(spec) },
                        label = { Text(spec) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = TextPrimary,
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedSpec == spec,
                            borderColor = Color(0xFFE0E0E0),
                            selectedBorderColor = AccentBlue,
                            borderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = if (spec != "All") {
                            { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.find_doctor_searching), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.common_error, error ?: ""), color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { 
                            viewModel.fetchDoctors()
                        }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val filteredDoctors = doctors.filter {
                                val matchesSearch = it.displayName.contains(searchQuery, ignoreCase = true) ||
                                                    (it.specialization?.contains(searchQuery, ignoreCase = true) ?: false)
                                val matchesSpec = selectedSpec == "All" || it.specialization == selectedSpec
                                matchesSearch && matchesSpec
                            }

                            if (doctors.isNotEmpty()) {
                                item { 
                                    Text(stringResource(R.string.find_doctor_registered), style = MaterialTheme.typography.titleSmall, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                                }
                                items(filteredDoctors) { doctor ->
                                    val status = connectionStatuses[doctor.uid]
                                    DoctorCard(
                                        doctor = doctor,
                                        status = status,
                                        onCardClick = { onDoctorClick(doctor, null) },
                                                onConnectClick = {
                                                    if (status == com.skinsense.ai.data.ConnectionStatus.ACCEPTED) {
                                                        onMessageClick(doctor)
                                                    } else {
                                                        viewModel.sendConnectionRequest(doctor) { success, msg ->
                                                            if (success) {
                                                                android.widget.Toast.makeText(context, context.getString(R.string.find_doctor_request_sent, doctor.displayName), android.widget.Toast.LENGTH_SHORT).show()
                                                                viewModel.refreshConnectionStatus(doctor.uid)
                                                            } else {
                                                                android.widget.Toast.makeText(context, msg ?: context.getString(R.string.find_doctor_request_failed), android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                    )
                                }
                            }

                        }
                }
            }
        }
    }
}

@Composable
fun DoctorCard(
    doctor: User, 
    status: com.skinsense.ai.data.ConnectionStatus? = null,
    onCardClick: () -> Unit,
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(0.dp), // Design shows minimal rounding or edge-to-edge
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (doctor.profileImageUri != null) {
                        AsyncImage(
                            model = doctor.profileImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            doctor.displayName.take(1).uppercase(),
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Row 1: Name + Connected Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dr. ${doctor.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        
                        if (status == com.skinsense.ai.data.ConnectionStatus.ACCEPTED) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "Connected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Row 2: Fee + Availability
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "₹${doctor.consultationFee}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•", color = Color.LightGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (doctor.isAvailableForBooking) "Available Today" else "Waitlist: 2 days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (doctor.isAvailableForBooking) Color(0xFF2E7D32) else TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Row 3: Specialization
                    Text(
                        text = doctor.specialization ?: "Specialist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Row 4: Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = doctor.rating.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${doctor.reviewCount} reviews)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Action Button on the far right
                val buttonText = when (status) {
                    com.skinsense.ai.data.ConnectionStatus.ACCEPTED -> "Message"
                    com.skinsense.ai.data.ConnectionStatus.PENDING -> "Pending"
                    else -> "Connect"
                }

                Button(
                    onClick = { if (status != com.skinsense.ai.data.ConnectionStatus.PENDING) onConnectClick() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == com.skinsense.ai.data.ConnectionStatus.ACCEPTED || status == com.skinsense.ai.data.ConnectionStatus.PENDING) 
                                            Color(0xFFE8EAF6) else AccentBlue,
                        contentColor = if (status == com.skinsense.ai.data.ConnectionStatus.ACCEPTED || status == com.skinsense.ai.data.ConnectionStatus.PENDING)
                                            AccentBlue else Color.White
                    ),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    enabled = status != com.skinsense.ai.data.ConnectionStatus.PENDING
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            // Divider between cards
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
        }
    }
}


