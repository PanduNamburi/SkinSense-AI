@file:OptIn(ExperimentalMaterial3Api::class)
package com.skinsense.ai.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import com.skinsense.ai.R
import com.skinsense.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    historyItems: List<HistoryItem>,
    onItemClick: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onDelete: (HistoryItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Scaffold(
        containerColor = MedicalBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.history_clear_all), tint = Color.Red.copy(alpha = 0.7f))
                        }
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
        ) {
            if (historyItems.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(historyItems) { index, item ->
                        var itemVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            itemVisible = true
                        }
                        
                        AnimatedVisibility(
                            visible = itemVisible,
                            enter = fadeIn(tween(800, delayMillis = index * 100)) + 
                                    slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(800, delayMillis = index * 100))
                        ) {
                            PremiumHistoryCard(
                                item = item,
                                onClick = { onItemClick(item) },
                                onDelete = { onDelete(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumHistoryCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium, // Changed from large to medium (18dp)
        color = MedicalSurface,
        border = borderLight()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Image Preview ---
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(72.dp),
                color = MedicalBackground
            ) {
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

                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = TextLight, modifier = Modifier.size(24.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // --- Info ---
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.diseaseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityBadge(severityLevel = item.severityLevel)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDate(item.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLight
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { item.confidence },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MedicalSecondary,
                    trackColor = MedicalSecondary.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = Color.Red.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MedicalSecondary.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MedicalSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.history_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

data class HistoryItem(
    val id: String,
    val diseaseName: String,
    val category: String,
    val severityLevel: SeverityLevel,
    val severityPercentage: Float,
    val confidence: Float,
    val date: Date,
    val imageUri: String? = null,
    val imageBase64: String? = null
)
