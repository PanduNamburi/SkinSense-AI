package com.skinsense.ai.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.data.OllamaChatMessage
import com.skinsense.ai.ui.ChatbotViewModel
import com.skinsense.ai.ui.theme.*
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────────────────────
// Color palette
// ──────────────────────────────────────────────────────────────────────────────
private val UserBubbleColor = MedicalSecondary
private val ScreenGradientStart = MedicalBackground
private val ScreenGradientEnd = Color.White
private val ThinkingDotColor = MedicalSecondary

@Composable
fun ChatbotScreen(
    viewModel: ChatbotViewModel
) {
    val messages = viewModel.messages
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty() || isLoading) {
            listState.animateScrollToItem(
                index = messages.size + (if (isLoading) 1 else 0)
            )
        }
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
                viewModel.clearError()
            }
        }
    }

    // Clear chat confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFE53935)
                )
            },
            title = { Text(stringResource(R.string.chat_clear_title)) },
            text = { Text(stringResource(R.string.chat_clear_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.chat_clear_btn), color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.chat_cancel_btn))
                }
            }
        )
    }

    fun onSend() {
        val text = inputText.trim()
        if (text.isNotEmpty()) {
            inputText = ""
            viewModel.sendMessage(text)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ScreenGradientStart, ScreenGradientEnd)
                    )
                )
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ──────────────────────────────────────────────────
                ChatbotHeader(
                    hasMessages = messages.isNotEmpty(),
                    onClearClick = { showClearDialog = true }
                )

                // ── Messages ────────────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty() && !isLoading) {
                        item { WelcomeCard() }
                    }

                    items(messages) { message ->
                        MessageBubble(message = message)
                    }

                    if (isLoading) {
                        item { TypingIndicator() }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }

                // ── Input Bar ───────────────────────────────────────────────
                ChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = ::onSend,
                    isLoading = isLoading
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Header with Groq branding + clear button
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatbotHeader(
    hasMessages: Boolean,
    onClearClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MedicalSecondary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MedicalSecondary,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.chat_header_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MedicalPrimary
                )
                Text(
                    text = stringResource(R.string.chat_header_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            // Clear chat button — only visible when there are messages
            if (hasMessages) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear chat",
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Welcome card
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun WelcomeCard() {
    val suggestions = listOf(
        "What causes acne?",
        "How do I treat eczema?",
        "What are signs of melanoma?",
        "Tips for dry skin in winter"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = AccentBlue.copy(alpha = 0.10f),
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Healing,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.chat_welcome_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.chat_welcome_desc),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.chat_try_asking),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        suggestions.windowed(2, 2, true).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { /* Set input text logic could be added here */ },
                        label = {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MedicalSecondary
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MedicalSecondary.copy(alpha = 0.08f)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = MedicalSecondary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Individual message bubble
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun MessageBubble(message: OllamaChatMessage) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) UserBubbleColor else Color.White
    val textColor = if (isUser) Color.White else MedicalPrimary
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = if (isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) // Premium 18dp corners
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }
    val horizontalPadding = if (isUser) PaddingValues(start = 56.dp) else PaddingValues(end = 56.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontalPadding),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = if (isUser) 2.dp else 1.dp,
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                lineHeight = 22.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Animated typing indicator
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 56.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    AnimatedDot(delayMillis = index * 160)
                }
            }
        }
    }
}

@Composable
private fun AnimatedDot(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_bounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis)
        ),
        label = "dot_offset"
    )

    Box(
        modifier = Modifier
            .size(9.dp)
            .offset(y = offsetY.dp)
            .clip(CircleShape)
            .background(ThinkingDotColor.copy(alpha = 0.7f))
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Input bar
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        stringResource(R.string.chat_input_placeholder),
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFF8FBFF),
                    unfocusedContainerColor = Color(0xFFF8FBFF)
                ),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(10.dp))

            val canSend = value.isNotBlank() && !isLoading
            Surface(
                shape = CircleShape,
                color = if (canSend) AccentBlue else Color(0xFFE0E0E0),
                modifier = Modifier.size(50.dp)
            ) {
                IconButton(
                    onClick = onSend,
                    enabled = canSend
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
