package com.skinsense.ai.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OllamaRepository {

    companion object {
        // 10.0.2.2 is the special Android emulator alias for the host machine (your Mac).
        // For a real device on the same WiFi, replace with your Mac's local IP (e.g., 192.168.1.105).
        private const val OLLAMA_BASE_URL = "http://10.0.2.2:11434"
        private const val TAG = "OllamaRepository"
        private const val MODEL_NAME = "llama3.2"

        private const val SYSTEM_PROMPT = """You are SkinSense AI, a friendly and knowledgeable dermatology assistant integrated into a skin disease prediction app. Your role is to:
1. Answer questions about skin conditions, their symptoms, causes, and triggers.
2. Explain what skin diseases like acne, eczema, psoriasis, rosacea, melanoma, and others are.
3. Provide general skincare advice and lifestyle tips.
4. Explain what to expect when visiting a dermatologist.
5. Help users understand their skin analysis results.

Important rules:
- NEVER provide a diagnosis — always remind users that the app's AI prediction is a screening tool, not a medical diagnosis.
- Always recommend consulting a licensed dermatologist for proper diagnosis and treatment.
- Be empathetic, clear, and use simple language suitable for non-medical users.
- Keep your answers concise (2-4 paragraphs max) unless asked for more detail.
- If a question is completely unrelated to skin health or dermatology, politely redirect the conversation."""
    }

    private val gson = Gson()

    /**
     * Sends the current conversation history to Ollama and returns the assistant's reply.
     * @param conversationHistory List of messages (excluding the system prompt).
     * @return The assistant's text reply, or null if the request failed.
     */
    suspend fun chat(conversationHistory: List<OllamaChatMessage>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Prepend the system prompt to every request
                val messages = buildList {
                    add(OllamaChatMessage(role = "system", content = SYSTEM_PROMPT))
                    addAll(conversationHistory)
                }

                val requestBody = gson.toJson(mapOf(
                    "model" to MODEL_NAME,
                    "messages" to messages,
                    "stream" to false
                ))

                Log.d(TAG, "Sending request to Ollama: $MODEL_NAME")

                val url = URL("$OLLAMA_BASE_URL/api/chat")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 120_000 // LLM can take a while

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    Log.e(TAG, "Ollama API error $responseCode: $errorBody")
                    return@withContext Result.failure(Exception("Ollama server returned error $responseCode. Is Ollama running? Try: OLLAMA_HOST=0.0.0.0 ollama serve"))
                }

                val responseText = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                // Parse: {"message": {"role": "assistant", "content": "..."}, ...}
                val jsonResponse = gson.fromJson(responseText, JsonObject::class.java)
                val content = jsonResponse
                    .getAsJsonObject("message")
                    ?.get("content")
                    ?.asString
                    ?: "Sorry, I couldn't parse the response."

                Log.d(TAG, "Ollama response received (${content.length} chars)")
                Result.success(content)

            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "Cannot connect to Ollama. Is it running?", e)
                Result.failure(Exception("Cannot connect to Ollama server.\n\nPlease start it on your Mac:\nOLLAMA_HOST=0.0.0.0 ollama serve"))
            } catch (e: Exception) {
                Log.e(TAG, "Ollama request failed", e)
                Result.failure(e)
            }
        }
    }
}
