package com.skinsense.ai.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.data.User
import com.skinsense.ai.data.UserRole
import com.skinsense.ai.ui.ProfileViewModel
import com.skinsense.ai.ui.ProfileUiState
import com.skinsense.ai.ui.theme.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    uid: String,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uid) {
        viewModel.loadProfile(uid)
    }

    LaunchedEffect(uiState) {
        val currentState = uiState
        when (currentState) {
            is ProfileUiState.Success -> {
                onComplete()
                viewModel.resetState()
            }
            is ProfileUiState.Error -> {
                // Show snackbar for upload, update, or storage-related errors
                val isUploadOrUpdateError = currentState.message.contains("photo", ignoreCase = true) || 
                                           currentState.message.contains("update", ignoreCase = true) || 
                                           currentState.message.contains("exist", ignoreCase = true) ||
                                           currentState.message.contains("location", ignoreCase = true) ||
                                           currentState.message.contains("storage", ignoreCase = true) ||
                                           currentState.message.contains("404", ignoreCase = true)
                
                if (isUploadOrUpdateError) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = currentState.message,
                            actionLabel = "Retry",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            currentState.lastAction?.invoke()
                        }
                    }
                }
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Complete Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MedicalPrimary
                )
            )
        },
        containerColor = MedicalBackground
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val currentState = uiState
            // Only show fullscreen error if it's NOT an "Updating/Upload/Storage" error
            val isSevereError = currentState is ProfileUiState.Error && 
                !(currentState.message.contains("photo", ignoreCase = true) || 
                  currentState.message.contains("update", ignoreCase = true) ||
                  currentState.message.contains("exist", ignoreCase = true) ||
                  currentState.message.contains("location", ignoreCase = true) ||
                  currentState.message.contains("storage", ignoreCase = true) ||
                  currentState.message.contains("404", ignoreCase = true))

            when {
                currentState is ProfileUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MedicalSecondary)
                    }
                }
                isSevereError -> {
                    val errorState = currentState as ProfileUiState.Error
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(errorState.message, color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(text = "Retry", onClick = { errorState.lastAction?.invoke() ?: viewModel.loadProfile(uid) })
                    }
                }
                else -> {
                    userProfile?.let { user ->
                        if (user.role == UserRole.DOCTOR) {
                            DoctorProfileForm(
                                user = user,
                                onSave = { updatedUser -> viewModel.updateProfile(updatedUser) },
                                onPhotoSelected = { uri -> viewModel.uploadProfilePhoto(uid, uri, context) }
                            )
                        } else {
                            PatientProfileForm(
                                user = user,
                                onSave = { updatedUser -> viewModel.updateProfile(updatedUser) },
                                onPhotoSelected = { uri -> viewModel.uploadProfilePhoto(uid, uri, context) }
                            )
                        }
                    }
                }
            }
            
            if (currentState is ProfileUiState.Updating) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PatientProfileForm(
    user: User, 
    onSave: (User) -> Unit,
    onPhotoSelected: (Uri) -> Unit
) {
    var username by remember { mutableStateOf(user.username ?: "") }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    var dob by remember { mutableStateOf(user.dateOfBirth ?: "") }
    var gender by remember { mutableStateOf(user.gender ?: "") }
    var location by remember { mutableStateOf(user.location ?: "") }
    var language by remember { mutableStateOf(user.language ?: "") }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var emergencyContact by remember { mutableStateOf(user.emergencyContact ?: "") }
    var bloodGroup by remember { mutableStateOf(user.bloodGroup ?: "") }
    var allergies by remember { mutableStateOf(user.allergies ?: "") }
    var skinConditions by remember { mutableStateOf(user.skinConditions ?: "") }
    var medications by remember { mutableStateOf(user.medications ?: "") }
    var medicalNotes by remember { mutableStateOf(user.medicalNotes ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ProfilePhotoPicker(
            currentPhotoUrl = user.profileImageUri,
            onPhotoSelected = onPhotoSelected
        )

        ProfileSection(title = "Profile Overview", icon = Icons.Default.Person) {
            PremiumTextField(value = username, onValueChange = { username = it }, label = "Username", placeholder = "Enter username", leadingIcon = Icons.Default.AlternateEmail)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = bio, onValueChange = { bio = it }, label = "Short Bio", placeholder = "Tell us about yourself", leadingIcon = Icons.Default.Info)
        }

        ProfileSection(title = "Personal Information", icon = Icons.Default.Badge) {
            PremiumTextField(value = dob, onValueChange = { dob = it }, label = "Date of Birth", placeholder = "YYYY-MM-DD", leadingIcon = Icons.Default.DateRange)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = gender, onValueChange = { gender = it }, label = "Gender", placeholder = "Male/Female/Other", leadingIcon = Icons.Default.Wc)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = location, onValueChange = { location = it }, label = "City / Location", placeholder = "Enter your city", leadingIcon = Icons.Default.LocationOn)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = language, onValueChange = { language = it }, label = "Language", placeholder = "English, Spanish, etc.", leadingIcon = Icons.Default.Language)
        }

        ProfileSection(title = "Contact Information", icon = Icons.Default.ContactPhone) {
            PremiumTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", placeholder = "+1234567890", leadingIcon = Icons.Default.Phone)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = emergencyContact, onValueChange = { emergencyContact = it }, label = "Emergency Contact", placeholder = "Name & Number", leadingIcon = Icons.Default.Emergency)
        }

        ProfileSection(title = "Health Information", icon = Icons.Default.MedicalServices) {
            PremiumTextField(value = bloodGroup, onValueChange = { bloodGroup = it }, label = "Blood Group", placeholder = "e.g. A+", leadingIcon = Icons.Default.WaterDrop)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = allergies, onValueChange = { allergies = it }, label = "Known Allergies", placeholder = "None, Peanuts, Penicillin...", leadingIcon = Icons.Default.Warning)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = skinConditions, onValueChange = { skinConditions = it }, label = "Existing Skin Conditions", placeholder = "Acne, Eczema...", leadingIcon = Icons.Default.Healing)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = medications, onValueChange = { medications = it }, label = "Ongoing Medications", placeholder = "List your current meds", leadingIcon = Icons.Default.Medication)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = medicalNotes, onValueChange = { medicalNotes = it }, label = "Medical Notes", placeholder = "Any other relevant info", leadingIcon = Icons.AutoMirrored.Filled.Notes)
        }

        PrimaryButton(
            text = "Save Profile",
            onClick = {
                onSave(user.copy(
                    username = username, bio = bio, dateOfBirth = dob, gender = gender,
                    location = location, language = language, phone = phone,
                    emergencyContact = emergencyContact, bloodGroup = bloodGroup,
                    allergies = allergies, skinConditions = skinConditions,
                    medications = medications, medicalNotes = medicalNotes
                ))
            }
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun DoctorProfileForm(
    user: User, 
    onSave: (User) -> Unit,
    onPhotoSelected: (Uri) -> Unit
) {
    var title by remember { mutableStateOf(user.medicalTitle ?: "") }
    var specialization by remember { mutableStateOf(user.specialization ?: "") }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    var license by remember { mutableStateOf(user.licenseNumber ?: "") }
    var experience by remember { mutableStateOf(user.experienceYears?.toString() ?: "") }
    var qualifications by remember { mutableStateOf(user.qualifications ?: "") }
    var hospital by remember { mutableStateOf(user.hospitalName ?: "") }
    var modes by remember { mutableStateOf(user.consultationModes ?: "") }
    var treated by remember { mutableStateOf(user.skinConditionsTreated ?: "") }
    var procedures by remember { mutableStateOf(user.proceduresOffered ?: "") }
    var interests by remember { mutableStateOf(user.areasOfInterest ?: "") }
    var address by remember { mutableStateOf(user.clinicAddress ?: "") }
    var location by remember { mutableStateOf(user.location ?: "") }
    var hours by remember { mutableStateOf(user.consultationHours ?: "") }
    var online by remember { mutableStateOf(user.onlineAvailability) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ProfilePhotoPicker(
            currentPhotoUrl = user.profileImageUri,
            onPhotoSelected = onPhotoSelected
        )
        
        ProfileSection(title = "Profile Overview", icon = Icons.Default.Person) {
            PremiumTextField(value = title, onValueChange = { title = it }, label = "Medical Title", placeholder = "Dr., MD, etc.", leadingIcon = Icons.Default.Title)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = specialization, onValueChange = { specialization = it }, label = "Specialization", placeholder = "Dermatologist, etc.", leadingIcon = Icons.Default.MedicalInformation)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = bio, onValueChange = { bio = it }, label = "Professional Bio", placeholder = "Brief professional background", leadingIcon = Icons.Default.Info)
        }

        ProfileSection(title = "Professional Details", icon = Icons.Default.Work) {
            PremiumTextField(value = license, onValueChange = { license = it }, label = "License Number", placeholder = "Enter medical license ID", leadingIcon = Icons.Default.VerifiedUser)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = experience, onValueChange = { experience = it }, label = "Years of Experience", placeholder = "e.g. 10", leadingIcon = Icons.Default.Timeline, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = qualifications, onValueChange = { qualifications = it }, label = "Qualifications", placeholder = "MBBS, MD, etc.", leadingIcon = Icons.Default.School)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = hospital, onValueChange = { hospital = it }, label = "Hospital / Clinic Name", placeholder = "Current place of practice", leadingIcon = Icons.Default.Apartment)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = modes, onValueChange = { modes = it }, label = "Consultation Modes", placeholder = "Chat / In-person / Both", leadingIcon = Icons.Default.Monitor)
        }

        ProfileSection(title = "Specialization & Expertise", icon = Icons.Default.Star) {
            PremiumTextField(value = treated, onValueChange = { treated = it }, label = "Conditions Treated", placeholder = "Acne, Psoriasis, etc.", leadingIcon = Icons.Default.Healing)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = procedures, onValueChange = { procedures = it }, label = "Procedures Offered", placeholder = "Laser, Chemical peel, etc.", leadingIcon = Icons.Default.SettingsAccessibility)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = interests, onValueChange = { interests = it }, label = "Areas of Interest", placeholder = "Pediatric dermatology, etc.", leadingIcon = Icons.Default.Favorite)
        }

        ProfileSection(title = "Contact & Practice Info", icon = Icons.Default.Business) {
            PremiumTextField(value = address, onValueChange = { address = it }, label = "Clinic Address", placeholder = "Full address", leadingIcon = Icons.Default.Map)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = location, onValueChange = { location = it }, label = "City / Location", placeholder = "City", leadingIcon = Icons.Default.LocationOn)
            Spacer(Modifier.height(12.dp))
            PremiumTextField(value = hours, onValueChange = { hours = it }, label = "Consultation Hours", placeholder = "Mon-Fri: 9AM - 5PM", leadingIcon = Icons.Default.Schedule)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = online, onCheckedChange = { online = it }, colors = CheckboxDefaults.colors(checkedColor = MedicalSecondary))
                Text("Online Consultation Available")
            }
        }

        PrimaryButton(
            text = "Save Professional Profile",
            onClick = {
                onSave(user.copy(
                    medicalTitle = title, specialization = specialization, bio = bio,
                    licenseNumber = license, experienceYears = experience.toIntOrNull(),
                    qualifications = qualifications, hospitalName = hospital,
                    consultationModes = modes, skinConditionsTreated = treated,
                    proceduresOffered = procedures, areasOfInterest = interests,
                    clinicAddress = address, location = location,
                    consultationHours = hours, onlineAvailability = online
                ))
            }
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ProfileSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassyCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MedicalSecondary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MedicalPrimary)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun ProfilePhotoPicker(
    currentPhotoUrl: String?,
    onPhotoSelected: (Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPhotoSelected(it) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (currentPhotoUrl != null) {
                AsyncImage(
                    model = currentPhotoUrl,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "Add Photo",
                    tint = MedicalSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (currentPhotoUrl == null) "Add Profile Photo" else "Change Photo",
            style = MaterialTheme.typography.labelMedium,
            color = MedicalSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}
