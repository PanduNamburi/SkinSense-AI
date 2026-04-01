package com.skinsense.ai.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.data.UserRole
import com.skinsense.ai.ui.theme.*

@Composable
fun AccessRoleSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.access_level_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoleItem(
                roleName = stringResource(R.string.role_patient),
                icon = Icons.Default.Person,
                isSelected = selectedRole == UserRole.PATIENT,
                onSelect = { onRoleSelected(UserRole.PATIENT) },
                modifier = Modifier.weight(1f)
            )
            RoleItem(
                roleName = stringResource(R.string.role_doctor),
                icon = Icons.Default.MedicalServices, // MedicalServices is close to stethoscope
                isSelected = selectedRole == UserRole.DOCTOR,
                onSelect = { onRoleSelected(UserRole.DOCTOR) },
                modifier = Modifier.weight(1f)
            )
            RoleItem(
                roleName = stringResource(R.string.role_admin),
                icon = Icons.Default.AdminPanelSettings,
                isSelected = selectedRole == UserRole.ADMIN,
                onSelect = { onRoleSelected(UserRole.ADMIN) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoleItem(
    roleName: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MedicalSecondary else Color.Transparent,
        label = "borderColor"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFFF1F5F9).copy(alpha = 0.5f),
        label = "bgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MedicalSecondary else TextMuted,
        label = "contentColor"
    )

    Surface(
        modifier = modifier
            .height(100.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 0.dp,
            color = borderColor
        ),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = roleName,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
