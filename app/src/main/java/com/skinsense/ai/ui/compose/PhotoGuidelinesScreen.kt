package com.skinsense.ai.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.ui.theme.*

@Composable
fun PhotoGuidelinesScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundMain,
        topBar = {
            GuidelinesTopBar(onBack = onBack)
        },
        bottomBar = {
            GuidelinesFooter(onContinue = onContinue)
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

            // Title Section
            Text(
                text = stringResource(R.string.guidelines_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.guidelines_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tips List
            GuidelineItem(
                icon = Icons.Default.Face,
                title = stringResource(R.string.guidelines_clear_title),
                description = stringResource(R.string.guidelines_clear_desc)
            )
            Spacer(modifier = Modifier.height(16.dp))
            GuidelineItem(
                icon = Icons.Default.Star,
                title = stringResource(R.string.guidelines_lighting_title),
                description = stringResource(R.string.guidelines_lighting_desc)
            )
            Spacer(modifier = Modifier.height(16.dp))
            GuidelineItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(R.string.guidelines_angles_title),
                description = stringResource(R.string.guidelines_angles_desc)
            )
            Spacer(modifier = Modifier.height(16.dp))
            GuidelineItem(
                icon = Icons.Default.Add,
                title = stringResource(R.string.guidelines_close_title),
                description = stringResource(R.string.guidelines_close_desc)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Metrics Summary
            GuidelinesMetrics()

            Spacer(modifier = Modifier.height(40.dp)) // Extra space
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidelinesTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.guidelines_top_bar),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundMain,
            titleContentColor = TextPrimary,
            navigationIconContentColor = TextPrimary
        )
    )
}

@Composable
fun GuidelineItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun GuidelinesMetrics() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundMain),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricCompact("10+", stringResource(R.string.metric_conditions))
                VerticalDivider()
                MetricCompact("97%", stringResource(R.string.metric_accuracy))
                VerticalDivider()
                MetricCompact("<60s", stringResource(R.string.metric_analysis))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                 Icon(
                     imageVector = Icons.Default.Info,
                     contentDescription = null,
                     tint = TextTertiary,
                     modifier = Modifier.size(12.dp)
                 )
                 Spacer(modifier = Modifier.width(4.dp))
                 Text(
                     text = "Results are indicative and for reference only.",
                     style = MaterialTheme.typography.labelSmall,
                     color = TextTertiary,
                     fontStyle = FontStyle.Italic
                 )
            }
        }
    }
}

@Composable
fun MetricCompact(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AccentBlue
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextTertiary,
            fontSize = 10.sp
        )
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(1.dp)
            .background(BorderLight)
    )
}

@Composable
fun GuidelinesFooter(onContinue: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = BackgroundCard
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(
                    text = stringResource(R.string.btn_continue_upload),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.guidelines_privacy),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
