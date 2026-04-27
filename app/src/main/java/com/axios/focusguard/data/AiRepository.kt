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

        // 1. Calculate Bursts (Group events by app and then by 10s proximity)
        val bursts = mutableListOf<List<SessionEvent>>()
        events.groupBy { it.packageName }.forEach { (_, appEvents) ->
            val sorted = appEvents.sortedBy { it.timestamp }
            var currentBurst = mutableListOf<SessionEvent>()
            
            sorted.forEach { event ->
                if (currentBurst.isEmpty()) {
                    currentBurst.add(event)
                } else {
                    val lastEvent = currentBurst.last()
                    if (event.timestamp - lastEvent.timestamp < 10000) {
                        currentBurst.add(event)
                    } else {
                        bursts.add(currentBurst)
                        currentBurst = mutableListOf(event)
                    }
                }
            }
            if (currentBurst.isNotEmpty()) bursts.add(currentBurst)
        }

        val totalBursts = bursts.size
        val totalRawAttempts = events.size
        val topCategory = events.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "OTHER"
        
        // 2. Identify frantic behavior
        val maxTapsInBurst = bursts.maxOfOrNull { it.size } ?: 0
        val franticInfo = if (maxTapsInBurst >= 5) {
            "Note: The user had a very frantic burst with $maxTapsInBurst taps in a single impulse."
        } else ""

        val prompt = """
            You are a productivity coach for a student using a Pomodoro focus app.
            Session Data:
            - Total Distraction Impulses (Bursts): $totalBursts
            - Total Raw Attempts (Taps): $totalRawAttempts
            - Top Distractor Category: $topCategory
            $franticInfo
            
            Provide a short, punchy, and slightly sarcastic analysis of their focus. 
            Mention the frantic behavior if applicable.
            Then, give 2 specific recommendations on how they can improve for the next session.
            
            CRITICAL: Respond in PLAIN TEXT ONLY. 
            No asterisks, no markdown, no bolding, no bullet symbols. 
            Use only clean prose and standard numbering (1., 2.).
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
