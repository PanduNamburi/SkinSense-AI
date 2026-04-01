@file:OptIn(ExperimentalMaterial3Api::class)
package com.skinsense.ai.ui.compose

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.res.stringResource
import com.skinsense.ai.R
import com.skinsense.ai.data.AnalysisResult
import com.skinsense.ai.ui.theme.*

@Composable
fun ResultsScreen(
    result: AnalysisResult,
    onFindSpecialist: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Scaffold(
        containerColor = MedicalBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.results_report), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MedicalBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Header Section / Primary Match ---
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + expandVertically(animationSpec = tween(800))
            ) {
                PremiumResultHeader(result)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Analyzed Photo ---
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 200))
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.results_sample),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    result.imageUri?.let { uri ->
                        Card(
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            border = borderLight()
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Analyzed Skin",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- About Section ---
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 400))
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.results_insight),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Symptoms Section ---
            if (result.symptoms.isNotEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800, delayMillis = 500))
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.results_symptoms),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        result.symptoms.forEach { symptom ->
                            ResultSymptomItem(symptom)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // --- Next Steps ---
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 600)) + slideInVertically(initialOffsetY = { 40 })
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Next Steps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onFindSpecialist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Consult a Dermatologist", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Disclaimer ---
            Surface(
                color = Warning.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, Warning.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Warning, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.results_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun PremiumResultHeader(result: AnalysisResult) {
    Surface(
        color = MedicalSurface,
        shape = MaterialTheme.shapes.large,
        border = borderLight(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeverityBadge(severityLevel = result.severityLevel)
                
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = MedicalSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = result.diseaseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${(result.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                    fontWeight = FontWeight.Black,
                    color = MedicalSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = stringResource(R.string.results_confidence),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLight
                    )
                    Text(
                        text = "Area: ${String.format("%.1f", result.severityPercentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ResultSymptomItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MedicalSurface, MaterialTheme.shapes.small)
            .border(borderLight(), MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MedicalAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

@Composable
fun PremiumNextStepsCard(diseaseName: String, onFindSpecialist: () -> Unit) {
    Surface(
        color = MedicalSecondary,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.results_consultation),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.results_confirmation, diseaseName),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onFindSpecialist,
                modifier = Modifier.fillMaxWidth().height(52.dp).animatePress(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MedicalSecondary),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.btn_locate_specialist), fontWeight = FontWeight.Bold)
            }
        }
    }
}
