package com.skinsense.ai.data

/**
 * Represents a single message in the conversation (aligns with the Ollama API format).
 * role: "system", "user", or "assistant"
 */
data class OllamaChatMessage(
    val role: String,
    val content: String
)
