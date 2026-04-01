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
 * Repository that connects to the Gemini API for skin-disease chatbot responses.
 * Uses the generateContent REST endpoint with a dermatology-focused system instruction.
 */
class GeminiChatRepository {

    companion object {
        private const val TAG = "GeminiChatRepository"
        private const val MODEL = "gemini-2.0-flash"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

        /**
         * System instruction injected into every request to keep responses focused on skin health.
         */
        private const val SYSTEM_INSTRUCTION = """You are SkinSense AI, a friendly and knowledgeable dermatology assistant built into a mobile skin disease prediction app.

Your job:
• Answer questions about skin conditions (acne, eczema, psoriasis, rosacea, melanoma, fungal infections, etc.)
• Explain symptoms, causes, triggers, and prevention strategies in simple, empathetic language
• Provide general skincare advice and lifestyle tips
• Help users understand their scan analysis results from the app
• Explain when and why to see a dermatologist

Rules you must follow:
• NEVER diagnose — always clarify you are an AI assistant and the app provides a screening tool, not a medical diagnosis
• Always recommend consulting a licensed dermatologist for actual diagnosis and treatment
• Keep answers concise (2–4 paragraphs) unless the user asks for more detail
• Use plain language suitable for non-medical users
• If a question is completely unrelated to skin or health, politely redirect to skin topics"""
    }

    private val gson = Gson()

    /**
     * Sends the conversation history to Gemini and returns the model's reply.
     *
     * @param history List of previous messages. Each entry has "role" ("user"/"model") and "content".
     * @return Result<String> — success with the text reply, or failure with an error.
     */
    suspend fun chat(history: List<OllamaChatMessage>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GOOGLE_AI_API_KEY
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Gemini API key not configured. Add GOOGLE_AI_API_KEY to local.properties.")
                    )
                }

                // Build Gemini contents array
                // Note: Gemini uses "model" for assistant role, not "assistant"
                val contents = history.map { msg ->
                    mapOf(
                        "role" to if (msg.role == "user") "user" else "model",
                        "parts" to listOf(mapOf("text" to msg.content))
                    )
                }

                val requestBody = gson.toJson(
                    mapOf(
                        "system_instruction" to mapOf(
                            "parts" to listOf(mapOf("text" to SYSTEM_INSTRUCTION))
                        ),
                        "contents" to contents,
                        "generationConfig" to mapOf(
                            "temperature" to 0.7,
                            "maxOutputTokens" to 1024
                        )
                    )
                )

                val url = URL("$BASE_URL?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText()
                        ?: "Unknown error"
                    Log.e(TAG, "Gemini API error $responseCode: $errorBody")
                    val errorJson = runCatching { gson.fromJson(errorBody, JsonObject::class.java) }
                        .getOrNull()
                    val errorMsg = errorJson?.getAsJsonObject("error")?.get("message")?.asString
                        ?: "API error $responseCode"
                    return@withContext Result.failure(Exception("Gemini: $errorMsg"))
                }

                val responseText = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                // Parse: candidates[0].content.parts[0].text
                val jsonResponse = gson.fromJson(responseText, JsonObject::class.java)
                val replyText = jsonResponse
                    .getAsJsonArray("candidates")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")
                    ?.get(0)?.asJsonObject
                    ?.get("text")?.asString
                    ?: "Sorry, I couldn't generate a response. Please try again."

                Log.d(TAG, "Gemini response received (${replyText.length} chars)")
                Result.success(replyText)

            } catch (e: Exception) {
                Log.e(TAG, "Gemini request failed", e)
                Result.failure(e)
            }
        }
    }
}
