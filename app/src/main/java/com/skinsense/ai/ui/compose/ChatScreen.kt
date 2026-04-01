@file:OptIn(ExperimentalMaterial3Api::class)
package com.skinsense.ai.ui.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.skinsense.ai.data.*
import com.skinsense.ai.ui.ChatViewModel
import com.skinsense.ai.ui.HistoryViewModel
import com.skinsense.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    navController: NavController,
    otherUserName: String,
    historyViewModel: HistoryViewModel
) {
    val messages by viewModel.messages.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showReportPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val historyItems by historyViewModel.historyItems.collectAsState()

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(context, it) }
    }

    // Show upload error in snackbar
    LaunchedEffect(uploadError) {
        uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUploadError()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        viewModel.markAsRead()
    }

    Scaffold(
        containerColor = MedicalBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = otherUserName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Online", style = MaterialTheme.typography.labelSmall, color = MedicalAccent)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MedicalBackground
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        val isCurrentUser = message.senderId == viewModel.currentUserId
                        PremiumMessageItem(
                            message = message,
                            isCurrentUser = isCurrentUser
                        )
                    }
                }

                // Uploading progress
                if (isUploading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MedicalSecondary
                    )
                }

                // Input Area
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MedicalSurface,
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { launcher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach Image", tint = MedicalSecondary)
                        }
                        
                        IconButton(onClick = { showReportPicker = true }) {
                            Icon(Icons.Default.Description, contentDescription = "Share Report", tint = MedicalSecondary)
                        }

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            placeholder = { Text("Type a message...", color = TextLight) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MedicalBackground,
                                unfocusedContainerColor = MedicalBackground,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MedicalSecondary
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(messageText)
                                    messageText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (messageText.isBlank()) TextLight.copy(alpha = 0.3f) else MedicalSecondary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (showReportPicker) {
                ModalBottomSheet(
                    onDismissRequest = { showReportPicker = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = MedicalBackground
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Share Analysis Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (historyItems.isEmpty()) {
                            Text("No reports found", color = TextLight, modifier = Modifier.padding(16.dp))
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                items(historyItems) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                viewModel.sendReport(
                                                    ReportAttachment(
                                                        diseaseName = item.diseaseName,
                                                        category = item.category ?: "Skin Disease",
                                                        severityLevel = item.severityLevel.name,
                                                        severityPercentage = item.severityPercentage,
                                                        confidence = item.confidence
                                                    )
                                                )
                                                showReportPicker = false
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MedicalSurface),
                                        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MedicalSecondary)
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(item.diseaseName, fontWeight = FontWeight.Bold)
                                                Text("${item.severityLevel} Severity • ${String.format("%.1f", item.severityPercentage)}% Area", style = MaterialTheme.typography.labelSmall, color = TextLight)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumMessageItem(message: ChatMessage, isCurrentUser: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInHorizontally(
            initialOffsetX = { if (isCurrentUser) 40 else -40 }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isCurrentUser) MedicalSecondary else MedicalSurface,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                ),
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 280.dp),
                border = if (isCurrentUser) null else borderLight()
            ) {
                Column(modifier = Modifier.padding(
                    horizontal = if (message.messageType == ChatMessageType.IMAGE) 0.dp else 16.dp,
                    vertical = if (message.messageType == ChatMessageType.IMAGE) 0.dp else 10.dp
                )) {
                    when (message.messageType) {
                        ChatMessageType.IMAGE -> {
                            AsyncImage(
                                model = message.attachmentUrl,
                                contentDescription = "Shared image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        ChatMessageType.REPORT -> {
                            message.reportAttachment?.let { report ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Assignment,
                                            contentDescription = null,
                                            tint = if (isCurrentUser) Color.White else MedicalSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Analysis Report",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentUser) Color.White else MedicalSecondary
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        report.diseaseName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCurrentUser) Color.White else TextMain
                                    )
                                    Text(
                                        "${report.severityLevel} Severity • ${String.format("%.1f", report.severityPercentage)}% Area",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else TextLight
                                    )
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = message.messageText,
                                color = if (isCurrentUser) Color.White else TextMain,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
            
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = TextLight,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
