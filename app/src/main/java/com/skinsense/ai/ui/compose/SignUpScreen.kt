@file:OptIn(ExperimentalMaterial3Api::class)

package com.skinsense.ai.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.data.UserRole
import com.skinsense.ai.ui.AuthState
import com.skinsense.ai.ui.AuthViewModel
import com.skinsense.ai.ui.theme.*

@Composable
fun RoleSignUpScreen(
    role: UserRole, // Only PATIENT or DOCTOR
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.PendingApproval || authState is AuthState.Success) {
            showSuccessDialog = true
        }
    }

    if (authState is AuthState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text(stringResource(R.string.signup_failed_title), fontWeight = FontWeight.Bold) },
            text = { Text((authState as AuthState.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text(stringResource(R.string.common_retry), color = MedicalSecondary)
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = Color.White
        )
    }

    if (showSuccessDialog) {
        if (role == UserRole.DOCTOR) {
            AlertDialog(
                onDismissRequest = {},
                icon = { Icon(Icons.Default.CheckCircle, null, tint = MedicalAccent, modifier = Modifier.size(48.dp)) },
                title = { Text(stringResource(R.string.signup_doctor_success_title), fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        stringResource(R.string.signup_doctor_success_msg),
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    PrimaryButton(text = stringResource(R.string.btn_return_login), onClick = { 
                        showSuccessDialog = false
                        viewModel.resetState()
                        onBack() 
                    }, modifier = Modifier.fillMaxWidth())
                },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = Color.White
            )
        } else {
            showSuccessDialog = false
        }
    }

    Scaffold(
        containerColor = MedicalBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back), tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (role == UserRole.PATIENT) {
            PatientSignUpForm(viewModel, paddingValues, onBack)
        } else {
            DoctorSignUpForm(viewModel, paddingValues, onBack)
        }
    }
}

@Composable
fun PatientSignUpForm(viewModel: AuthViewModel, paddingValues: PaddingValues, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    val genders = listOf(stringResource(R.string.gender_male), stringResource(R.string.gender_female), stringResource(R.string.gender_other))
    var gender by remember { mutableStateOf(genders[0]) }
    var genderExpanded by remember { mutableStateOf(false) }
    var healthConsent by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(1000)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(1000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.signup_title_patient),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = stringResource(R.string.signup_subtitle_patient),
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(32.dp))

            PremiumTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.common_full_name),
                placeholder = stringResource(R.string.signup_placeholder_name),
                leadingIcon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(20.dp))

            PremiumTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.common_email),
                placeholder = "name@example.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(20.dp))

            PremiumTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.common_password),
                placeholder = stringResource(R.string.signup_placeholder_password),
                leadingIcon = Icons.Default.Lock,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Gender Dropdown
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.common_gender),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMain,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalSecondary,
                                unfocusedBorderColor = MedicalSecondary.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            genders.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        gender = item
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Age TextField
                Column(modifier = Modifier.weight(1f)) {
                    PremiumTextField(
                        value = age,
                        onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
                        label = stringResource(R.string.common_age),
                        placeholder = stringResource(R.string.age_placeholder),
                        leadingIcon = Icons.Default.Cake,
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = MedicalSurface,
                shape = MaterialTheme.shapes.medium,
                border = borderLight()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.signup_health_consent), style = MaterialTheme.typography.titleSmall, color = TextMain)
                        Text(stringResource(R.string.signup_consent_desc), style = MaterialTheme.typography.labelSmall, color = TextLight)
                    }
                    Switch(
                        checked = healthConsent,
                        onCheckedChange = { healthConsent = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MedicalSecondary,
                            uncheckedTrackColor = MedicalSecondary.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            PrimaryButton(
                text = stringResource(R.string.signup_btn),
                onClick = { 
                    viewModel.signUpPatient(name.trim(), email.trim(), password, "Not Provided", age, gender) 
                },
                enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6 && age.isNotBlank() && healthConsent,
                loading = authState is AuthState.Loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.signup_already_account), color = TextLight, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(R.string.signup_login_link),
                    color = MedicalSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBack() }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DoctorSignUpForm(viewModel: AuthViewModel, paddingValues: PaddingValues, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    val specializations = listOf(
        stringResource(R.string.spec_dermatologist),
        stringResource(R.string.spec_gp),
        stringResource(R.string.spec_surgeon),
        stringResource(R.string.spec_esthetician),
        stringResource(R.string.spec_researcher)
    )
    var specialization by remember { mutableStateOf(specializations[0]) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(1000)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(1000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.signup_title_doctor_portal),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = stringResource(R.string.signup_subtitle_doctor),
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(32.dp))

            PremiumTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                placeholder = stringResource(R.string.signup_placeholder_doctor_name),
                leadingIcon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(20.dp))

            PremiumTextField(
                value = licenseNumber,
                onValueChange = { licenseNumber = it },
                label = "Medical License",
                placeholder = stringResource(R.string.signup_placeholder_license),
                leadingIcon = Icons.Default.Badge
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.signup_specialization_label),
                style = MaterialTheme.typography.labelMedium,
                color = TextMain,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = specialization,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedicalSecondary,
                        unfocusedBorderColor = MedicalSecondary.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    specializations.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                specialization = item
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PremiumTextField(
                value = email,
                onValueChange = { email = it },
                label = "Professional Email",
                placeholder = "doctor@hospital.org",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(20.dp))

            PremiumTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.signup_password_label),
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(40.dp))

            PrimaryButton(
                text = stringResource(R.string.btn_complete_registration),
                onClick = {
                    viewModel.signUpDoctor(
                        name = name,
                        email = email,
                        password = password,
                        specialization = specialization,
                        qualifications = "Not Provided",
                        experienceYears = 2,
                        hospitalName = "Not Provided",
                        contactDetails = "Not Provided",
                        licenseNumber = licenseNumber
                    )
                },
                enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6 && licenseNumber.isNotBlank(),
                loading = authState is AuthState.Loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
