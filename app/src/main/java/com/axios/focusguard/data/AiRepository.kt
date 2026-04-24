package com.axios.focusguard.data

import com.axios.focusguard.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor() {

    // API Key - Ensure this is active in AI Studio
    private val apiKey = BuildConfig.GEMINI_API_KEY

    // Use "gemini-2.5-flash" or "gemini-flash-latest" as they are the currently available stable models.
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    suspend fun getSessionAnalysis(events: List<SessionEvent>): String = withContext(Dispatchers.IO) {
        if (apiKey.startsWith("YOUR_GEMINI")) {
            return@withContext "AI analysis is ready, but you need to add your Gemini API key in AiRepository.kt to see it!"
        }

        if (events.isEmpty()) {
            return@withContext "Perfect focus! You didn't try to open any distracting apps. You're a productivity machine. Keep this momentum for your next session!"
        }

        val appAttempts = events.groupBy { it.appName }.mapValues { it.value.size }
        val prompt = """
            You are a productivity coach for a student using a Pomodoro focus app.
            During a 25-minute focus session, the user tried to open the following distracting apps:
            ${appAttempts.entries.joinToString { "${it.key}: ${it.value} times" }}
            
            Provide a short, punchy, and slightly sarcastic analysis of their focus. 
            Then, give 2 specific recommendations on how they can improve for the next session.
            Keep the total response under 100 words.
        """.trimIndent()

        try {
            val response = model.generateContent(content { text(prompt) })
            response.text ?: "The AI was too impressed by your focus to speak."
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("404")) {
                "Gemini Error (404): The model name is likely incorrect or your key doesn't have access to it. \n\n" +
                "To fix this:\n" +
                "1. Go to https://aistudio.google.com\n" +
                "2. Click 'Create New' -> 'Chat Prompt'\n" +
                "3. Look at the 'Model' dropdown in the top right. Use THAT exact name (e.g., 'gemini-2.5-flash').\n" +
                "4. Current attempt used: 'gemini-2.5-flash'"
            } else {
                "Error reaching the productivity oracle: ${e.localizedMessage}"
            }
        }
    }
}
