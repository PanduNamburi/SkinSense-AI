package com.skinsense.ai.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skinsense.ai.ui.AuthViewModel
import com.skinsense.ai.ui.AuthState
import com.skinsense.ai.ui.theme.*
import com.skinsense.ai.utils.BiometricHelper
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import android.util.Log
import com.skinsense.ai.R
import com.skinsense.ai.utils.LocaleHelper
import android.content.Intent

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel = viewModel(),
    onCompleteProfile: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onMyAppointmentsClick: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context as FragmentActivity) }
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.updateBiometricAvailability(biometricHelper.isBiometricAvailable())
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.profile_delete_title), fontWeight = FontWeight.Bold) },
            text = { 
                Text(stringResource(R.string.profile_delete_msg)) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.profile_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { langCode ->
                showLanguageDialog = false
                LocaleHelper.setLocale(context, langCode)
                // Restart activity
                val intent = Intent(context, com.skinsense.ai.MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MedicalBackground)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header Section ---
        Spacer(modifier = Modifier.height(60.dp))
        
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(130.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
            ) {
                if (userProfile?.profileImageUri != null) {
                    val uri = userProfile?.profileImageUri!!
                    val imageModel: Any = remember(uri) {
                        try {
                            if (uri.startsWith("data:image")) {
                                val base64Data = uri.substringAfter("base64,")
                                android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                            } else {
                                uri
                            }
                        } catch (e: Exception) {
                            uri
                        }
                    }

                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(imageModel)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_branding_logo)
                            .error(R.drawable.ic_branding_logo)
                            .build(),
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MedicalPrimary,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            // Floating Edit Button Small
            Surface(
                onClick = onCompleteProfile,
                shape = CircleShape,
                color = OrangeAccent,
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .shadow(elevation = 4.dp, shape = CircleShape),
                border = BorderStroke(2.dp, Color.White)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit, 
                        contentDescription = "Edit", 
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = userProfile?.displayName ?: "User",
            style = MaterialTheme.typography.headlineSmall,
            color = MedicalPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = userProfile?.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Patient",
            style = MaterialTheme.typography.bodyLarge,
            color = TextLight,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // EDIT PROFILE Wide Button
        Button(
            onClick = onCompleteProfile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5EBF5)),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = stringResource(R.string.profile_edit_btn), 
                color = MedicalPrimary, 
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // --- Stats Row (Card Based) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard(modifier = Modifier.weight(1f), label = stringResource(R.string.common_gender), value = userProfile?.gender ?: "Male")
            ProfileStatCard(modifier = Modifier.weight(1f), label = stringResource(R.string.common_age), value = calculateAge(userProfile?.dateOfBirth) ?: "23")
            ProfileStatCard(modifier = Modifier.weight(2f), label = stringResource(R.string.profile_patient_id), value = userProfile?.uid?.takeLast(8)?.uppercase()?.let { "SS-$it" } ?: "SS-BARI64ZV")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Medical Details Section ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.profile_medical_details),
                    style = MaterialTheme.typography.titleMedium,
                    color = MedicalPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.profile_view_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewHistory() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download Records Button (Orange/Cream theme)
            Surface(
                onClick = { /* Implement Download */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = OrangeLight,
                border = BorderStroke(1.dp, OrangeAccent.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description, 
                        contentDescription = null, 
                        tint = OrangeAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        stringResource(R.string.profile_download_records), 
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.FileDownload, 
                        contentDescription = null, 
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // My Appointments Button
            Surface(
                onClick = onMyAppointmentsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MedicalSecondary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MedicalSecondary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday, 
                        contentDescription = null, 
                        tint = MedicalSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "My Appointments", 
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MedicalSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight, 
                        contentDescription = null, 
                        tint = MedicalSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Grid for Medical Info with Icons
            Row(modifier = Modifier.fillMaxWidth()) {
                MedicalDetailCardIteration2(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_chronic_conditions),
                    value = userProfile?.skinConditions?.takeIf { it.isNotBlank() } ?: "None reported",
                    icon = Icons.Default.Star // Placeholder for star/asterisk
                )
                Spacer(modifier = Modifier.width(12.dp))
                MedicalDetailCardIteration2(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_medications),
                    value = userProfile?.medications?.takeIf { it.isNotBlank() } ?: "None reported",
                    icon = Icons.Default.MedicalServices // Pill icon
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MedicalDetailCardIteration2(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_allergies),
                    value = userProfile?.allergies?.takeIf { it.isNotBlank() } ?: "No allergies",
                    icon = Icons.Default.Warning // Warning icon
                )
                Spacer(modifier = Modifier.width(12.dp))
                MedicalDetailCardIteration2(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_primary_doctor),
                    value = "Dr. Sarah Smith",
                    icon = Icons.Default.AssignmentInd // Doctor/Profile icon
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Security Settings ---
            Text(
                text = stringResource(R.string.profile_security_settings),
                style = MaterialTheme.typography.titleMedium,
                color = MedicalPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column {
                    SecuritySettingRow(
                        icon = Icons.Default.Fingerprint,
                        title = stringResource(R.string.profile_biometric_login),
                        content = {
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) viewModel.disableBiometric() else viewModel.signOut()
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = OrangeAccent)
                            )
                        }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    SecuritySettingRow(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.profile_language),
                        content = {
                            Row(
                                modifier = Modifier.clickable { showLanguageDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when(LocaleHelper.getLanguage(context)) {
                                        "hi" -> "हिन्दी"
                                        "te" -> "తెలుగు"
                                        "ta" -> "தமிழ்"
                                        "kn" -> "ಕನ್ನಡ"
                                        "ml" -> "മലയാളം"
                                        else -> "English"
                                    },
                                    color = MedicalSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextLight)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Account Actions (Sign Out, Delete)
            Button(
                onClick = { viewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MedicalPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.profile_sign_out), fontWeight = FontWeight.Bold, color = MedicalPrimary)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_delete_account), color = Color.Red.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun ProfileStatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Surface(
        modifier = modifier.height(75.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextLight, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = MedicalPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MedicalDetailCardIteration2(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = modifier
            .height(105.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = OrangeAccent.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MedicalPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SecuritySettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextLight, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MedicalPrimary, fontWeight = FontWeight.Medium)
        }
        content()
    }
}

private fun calculateAge(dob: String?): String? {
    if (dob.isNullOrBlank()) return null
    if (dob.all { it.isDigit() }) return dob // Return directly if it's already an age number
    
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val birthDate = sdf.parse(dob) ?: return null
        val today = java.util.Calendar.getInstance()
        val birthCalendar = java.util.Calendar.getInstance().apply { time = birthDate }
        
        var age = today.get(java.util.Calendar.YEAR) - birthCalendar.get(java.util.Calendar.YEAR)
        if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
            age--
        }
        age.toString()
    } catch (e: Exception) {
        null
    }
}

