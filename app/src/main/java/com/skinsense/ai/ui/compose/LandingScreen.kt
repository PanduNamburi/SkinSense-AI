package com.skinsense.ai.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.ui.theme.AccentBlue
import com.skinsense.ai.ui.theme.PrimaryDark
import com.skinsense.ai.utils.LocaleHelper
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Language

@Composable
fun LandingScreen(
    onPatientSelected: () -> Unit,
    onDoctorSelected: () -> Unit,
    onAdminSelected: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedRole by remember { mutableStateOf<Role?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
    ) {
        // --- Header Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_landing),
                contentDescription = "Medical Technology",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay (White fade at bottom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.7f),
                                Color.White
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Header Text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.landing_precision),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B) // Dark Slate
                )
                Text(
                    text = stringResource(R.string.landing_dermatology),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6) // Bright Blue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.landing_tagline),
                    fontSize = 16.sp,
                    color = Color(0xFF64748B), // Slate Gray
                    lineHeight = 22.sp
                )
            }

            // Language Picker Button (Top Right)
            IconButton(
                onClick = { showLanguageDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Select Language",
                    tint = Color(0xFF1E293B)
                )
            }
        }

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                onDismiss = { showLanguageDialog = false },
                onLanguageSelected = { langCode ->
                    showLanguageDialog = false
                    LocaleHelper.setLocale(context, langCode)
                    // Restart activity to apply language change
                    val intent = Intent(context, com.skinsense.ai.MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            )
        }

        // --- Content Section ---
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.landing_welcome),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = stringResource(R.string.landing_select_profile),
                fontSize = 15.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Role Cards
            RoleCard(
                icon = Icons.Default.Person,
                title = stringResource(R.string.role_patient),
                subtitle = stringResource(R.string.role_patient_desc),
                isSelected = selectedRole == Role.PATIENT,
                onClick = { selectedRole = Role.PATIENT }
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            RoleCard(
                icon = Icons.Default.MedicalServices, // Changed to MedicalServices
                title = stringResource(R.string.role_doctor),
                subtitle = stringResource(R.string.role_doctor_desc),
                isSelected = selectedRole == Role.DOCTOR,
                onClick = { selectedRole = Role.DOCTOR }
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            RoleCard(
                icon = Icons.Default.AdminPanelSettings,
                title = stringResource(R.string.role_admin), // Changed to Administrator
                subtitle = stringResource(R.string.role_admin_desc),
                isSelected = selectedRole == Role.ADMIN,
                onClick = { selectedRole = Role.ADMIN }
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Continue Button
            Button(
                onClick = {
                    when (selectedRole) {
                        Role.PATIENT -> onPatientSelected()
                        Role.DOCTOR -> onDoctorSelected()
                        Role.ADMIN -> onAdminSelected()
                        null -> {} // Should not happen as button is disabled
                    }
                },
                enabled = selectedRole != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF), // iOS Blue style
                    disabledContainerColor = Color(0xFFE2E8F0),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_continue),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private enum class Role {
    PATIENT, DOCTOR, ADMIN
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val borderColor = if (isSelected) Color(0xFF3B82F6) else Color(0xFFF1F5F9)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                scale = 0.98f
                onClick()
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White), // Keep card white, tint via overlay or just border
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFEFF6FF), // Blue when selected, Light Blue when not?
                // actually reference image shows Blue Icon on Blue BG (Patient). Let's stick to reference style.
                // Reference: Blue icon on light blue bg.
                // Let's keep icon consistent but maybe highlight container?
                // Reference image: Icon container is solid blue with WHITE icon for Patient? No, looks like Blue Icon on Light Blue.
                // Wait, "Patient" card in user image: Solid Blue Icon container with White Icon.
                // Let's try that for selected state.
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(
                         if (isSelected) Color(0xFF3B82F6) else Color(0xFFEFF6FF)
                    )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color(0xFF3B82F6),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF3B82F6), // Blue 500
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
