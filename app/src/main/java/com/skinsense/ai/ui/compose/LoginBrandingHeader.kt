package com.skinsense.ai.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.ui.theme.*

@Composable
fun LoginBrandingHeader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Medical Logo Box (The Blue + Icon) ---
        Surface(
            modifier = Modifier
                .size(100.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), spotColor = MedicalSecondary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.ic_branding_logo),
                    contentDescription = "SkinSense AI Logo",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SkinSense AI Title ---
        Text(
            text = stringResource(R.string.footer_branding),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TextMain,
            letterSpacing = (-0.5).sp
        )
        
        // --- Subtitle ---
        Text(
            text = stringResource(R.string.login_branding_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
