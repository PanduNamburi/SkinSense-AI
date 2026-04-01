package com.skinsense.ai.ui.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.skinsense.ai.data.*
import com.skinsense.ai.ui.ConsultationChatViewModel
import com.skinsense.ai.ui.HistoryViewModel
import com.skinsense.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationChatScreen(
    currentUserId: String,
    viewModel: ConsultationChatViewModel,
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showReportPicker by remember { mutableStateOf(false) }
    val historyItems by historyViewModel.historyItems.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = androidx.compose.ui.platform.LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(context, it) }
    }

    LaunchedEffect(uploadError) {
        uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUploadError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Consultation Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundMain
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            isCurrentUser = message.senderId == currentUserId
                        )
                    }
                }

                if (isUploading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AccentBlue)
                }

                // Input Area
                Surface(
                    tonalElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { launcher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach Image", tint = AccentBlue)
                        }
                        
                        IconButton(onClick = { showReportPicker = true }) {
                            Icon(Icons.Default.Description, contentDescription = "Share Report", tint = AccentBlue)
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color(0xFFF1F5F9),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            containerColor = AccentBlue,
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }

            if (showReportPicker) {
                ModalBottomSheet(
                    onDismissRequest = { showReportPicker = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = Color.White
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
                            Text("No reports found", color = TextSecondary, modifier = Modifier.padding(16.dp))
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
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Assignment, contentDescription = null, tint = AccentBlue)
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(item.diseaseName, fontWeight = FontWeight.Bold)
                                                Text("${item.severityLevel} Severity • ${String.format("%.1f", item.severityPercentage)}% Area", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
fun MessageBubble(message: ConsultationMessage, isCurrentUser: Boolean) {
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isCurrentUser) AccentBlue else Color(0xFFE2E8F0)
    val textColor = if (isCurrentUser) Color.White else TextPrimary
    val shape = if (isCurrentUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 116.dp, 16.dp, 2.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start) {
            Surface(
                color = bgColor,
                shape = shape,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(
                    horizontal = if (message.messageType == "IMAGE") 0.dp else 16.dp,
                    vertical = if (message.messageType == "IMAGE") 0.dp else 10.dp
                )) {
                    when (message.messageType) {
                        "IMAGE" -> {
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
                        "REPORT" -> {
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
                                            tint = if (isCurrentUser) Color.White else AccentBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Analysis Report",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentUser) Color.White else AccentBlue
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        report.diseaseName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCurrentUser) Color.White else TextPrimary
                                    )
                                    Text(
                                        "${report.severityLevel} Severity • ${String.format("%.1f", report.severityPercentage)}% Area",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else TextSecondary
                                    )
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = message.text,
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            Text(
                text = if (isCurrentUser) "You" else message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}
