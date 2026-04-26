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

        val totalEvents = events.size
        val categories = events.groupBy { it.category }.mapValues { it.value.size }
        val appAttempts = events.groupBy { it.appName }.mapValues { it.value.size }
        
        // Timing breakdown: Early (first 1/3), Mid (second 1/3), Late (last 1/3)
        // Assuming 25 min default if initialSessionSeconds not available in event
        val timingBreakdown = events.groupBy { 
            when {
                it.sessionOffsetSeconds < 500 -> "Early"
                it.sessionOffsetSeconds < 1000 -> "Mid"
                else -> "Late"
            }
        }.mapValues { it.value.size }

        val prompt = """
            You are a productivity coach for a user using a focus app.
            In this session, the user had $totalEvents distraction bursts (consecutive attempts grouped).
            
            App Breakdown:
            ${appAttempts.entries.joinToString { "${it.key}: ${it.value} bursts" }}
            
            Category Breakdown:
            ${categories.entries.joinToString { "${it.key}: ${it.value} events" }}
            
            Timing Distribution:
            ${timingBreakdown.entries.joinToString { "${it.key}: ${it.value} distractions" }}
            
            Provide a short, punchy, and slightly sarcastic analysis of their focus. 
            Identify if they crack early or lose steam at the end.
            Mention if they gravitate towards specific categories like SOCIAL or VIDEO.
            Then, give 2 specific recommendations for the next session.
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
