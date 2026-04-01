package com.skinsense.ai.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.skinsense.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repository that connects to the Groq API for skin-disease chatbot responses.
 * Groq is OpenAI-compatible and offers a very generous free tier with Llama 3.
 * Sign up at: https://console.groq.com
 */
class GroqChatRepository {

    companion object {
        private const val TAG = "GroqChatRepository"
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL = "llama-3.3-70b-versatile" // Best free model on Groq

        private const val SYSTEM_PROMPT = """You are SkinSense AI, a friendly and knowledgeable dermatology assistant built into a mobile skin disease prediction app.

Your job:
• Answer questions about skin conditions (acne, eczema, psoriasis, rosacea, melanoma, fungal infections, etc.)
• Explain symptoms, causes, triggers, and prevention in simple, empathetic language
• Provide general skincare advice and lifestyle tips
• Help users understand their skin scan results from the app
• Explain when and why to see a dermatologist

Rules you must follow:
• NEVER diagnose — clarify you are an AI assistant and the app provides a screening tool, not a medical diagnosis
• Always recommend consulting a licensed dermatologist for actual diagnosis and treatment
• Keep answers concise (2–4 paragraphs) unless the user asks for more detail
• Use plain language suitable for non-medical users
• If a question is completely unrelated to skin health, politely redirect to skin topics"""
    }

    private val gson = Gson()

    /**
     * Sends the conversation history to Groq and returns the model's reply.
     * @param history User/assistant message history (OllamaChatMessage reused as generic msg model).
     * @return Result<String> — success with the text reply, or failure with an error.
     */
    suspend fun chat(history: List<OllamaChatMessage>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GROQ_API_KEY
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Groq API key not set.\n\nAdd GROQ_API_KEY=gsk_... to local.properties\nGet a free key at console.groq.com")
                    )
                }

                // Build messages array: prepend system prompt
                val messages = buildList {
                    add(mapOf("role" to "system", "content" to SYSTEM_PROMPT))
                    history.forEach { msg ->
                        add(mapOf("role" to msg.role, "content" to msg.content))
                    }
                }

                val requestBody = gson.toJson(
                    mapOf(
                        "model" to MODEL,
                        "messages" to messages,
                        "temperature" to 0.7,
                        "max_tokens" to 1024
                    )
                )

                val url = URL(GROQ_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    Log.e(TAG, "Groq API error $responseCode: $errorBody")
                    val errorJson = runCatching { gson.fromJson(errorBody, JsonObject::class.java) }.getOrNull()
                    val errorMsg = errorJson?.getAsJsonObject("error")?.get("message")?.asString
                        ?: "API error $responseCode"
                    return@withContext Result.failure(Exception("Groq: $errorMsg"))
                }

                val responseText = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                // Parse OpenAI-compatible response: choices[0].message.content
                val jsonResponse = gson.fromJson(responseText, JsonObject::class.java)
                val replyText = jsonResponse
                    .getAsJsonArray("choices")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString
                    ?: "Sorry, I couldn't generate a response. Please try again."

                Log.d(TAG, "Groq response received (${replyText.length} chars)")
                Result.success(replyText)

            } catch (e: Exception) {
                Log.e(TAG, "Groq request failed", e)
                Result.failure(e)
            }
        }
    }
}
