package com.skinsense.ai.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import com.skinsense.ai.data.DiseaseRepository
import com.skinsense.ai.ui.HistoryViewModel
import com.skinsense.ai.ui.theme.*

@Composable
fun HistoryDetailScreen(
    historyId: String,
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    var historyItem by remember { mutableStateOf<HistoryItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(historyId) {
        historyItem = viewModel.getHistoryItemById(historyId)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentBlue)
        }
    } else {
        historyItem?.let { item ->
            // Fetch disease details for description and symptoms since HistoryItem might not have them all populated deeply
            // or we need to fetch them again from Repository based on name.
            val disease = remember(item.diseaseName) {
                DiseaseRepository.getDiseaseByName(item.diseaseName)
            }

            HistoryDetailContent(
                item = item,
                description = disease?.description ?: "No description available.",
                symptoms = disease?.symptoms ?: emptyList(),
                onBack = onBack
            )
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Item not found", color = TextSecondary)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Go Back")
                }
            }
        }
    }
}

@Composable
fun HistoryDetailContent(
    item: HistoryItem,
    description: String,
    symptoms: List<String>,
    onBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMain)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Analysis Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // --- Header Section ---
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -40 }, animationSpec = tween(600))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), spotColor = AccentBlue.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeverityBadge(severityLevel = item.severityLevel)
                        
                        Text(
                            text = "Area: ${String.format("%.1f", item.severityPercentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrangeAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.diseaseName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${(item.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confidence Score",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Analyzed Photo ---
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600, delayMillis = 100))
        ) {
            Column {
                Text(
                    text = "Saved Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                val imageModel: Any? = remember(item.imageBase64, item.imageUri) {
                    try {
                        val base64 = item.imageBase64
                        if (base64 != null && base64.startsWith("data:image")) {
                            val base64Data = base64.substringAfter("base64,")
                            android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        } else {
                            item.imageUri
                        }
                    } catch (e: Exception) {
                        item.imageUri
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Analyzed Skin",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- About Section ---
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(600, delayMillis = 200))) {
            Column {
                SectionHeader(icon = Icons.Default.Info, title = "About the Condition", iconColor = MedicalSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Common Symptoms ---
        if (symptoms.isNotEmpty()) {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(600, delayMillis = 300))) {
                Column {
                    SectionHeader(icon = Icons.AutoMirrored.Filled.List, title = "Common Symptoms", iconColor = MedicalSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    symptoms.forEach { symptom ->
                        SymptomItem(text = symptom)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Disclaimer ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MEDICAL DISCLAIMER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This is a past analysis record. It is for informational purposes only and does not constitute a professional medical diagnosis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
