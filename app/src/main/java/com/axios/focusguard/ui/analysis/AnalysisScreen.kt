package com.axios.focusguard.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.axios.focusguard.ui.components.MascotImage
import com.axios.focusguard.ui.components.MascotPose
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onNextSession: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNextSession) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.analysisText.isNotEmpty()) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(uiState.analysisText))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(onClick = {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, uiState.analysisText)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    MascotImage(pose = MascotPose.THINKING, size = 150.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Deconstructing your focus...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                // Focus Score Hero
                val burstCount = countBursts(uiState.events)
                val focusScore = calculateFocusScore(uiState.events, burstCount)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("FOCUS SCORE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(
                            text = focusScore.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = getScoreLabel(focusScore),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Category Breakdown
                if (uiState.events.isNotEmpty()) {
                    Text(
                        text = "Category Split",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CategorySegmentedBar(uiState.events)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Timing Breakdown
                    Text(
                        text = "Timing Patterns",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TimingBreakdownGrid(uiState.events)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // AI Insights centerpiece
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "The Coach's Take",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    MascotImage(pose = MascotPose.THINKING, size = 48.dp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = uiState.analysisText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 26.sp,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onNextSession,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("START NEXT SESSION", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CategorySegmentedBar(events: List<com.axios.focusguard.data.SessionEvent>) {
    val total = events.size.toFloat()
    val groups = events.groupBy { it.category }
    
    val colors = mapOf(
        "SOCIAL" to Color(0xFFD97767), // Terra Cotta
        "VIDEO" to Color(0xFFE9C46A),  // Saffron
        "GAME" to Color(0xFF8367C7),   // Muted Purple
        "OTHER" to MaterialTheme.colorScheme.outline
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                for (entry in groups) {
                    val catEvents = entry.value
                    val cat = entry.key
                    Box(
                        modifier = Modifier
                            .weight(catEvents.size.toFloat())
                            .fillMaxHeight()
                            .background(colors[cat] ?: MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val entries = groups.entries.toList()
            for (index in entries.indices) {
                val entry = entries[index]
                val cat = entry.key
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(colors[cat] ?: MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(cat, style = MaterialTheme.typography.labelSmall)
                }
                if (index < entries.size - 1) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

@Composable
fun TimingBreakdownGrid(events: List<com.axios.focusguard.data.SessionEvent>) {
    val early = events.count { it.sessionOffsetSeconds < 300 }
    val late = events.count { it.sessionOffsetSeconds > 1200 }
    val mid = (events.size - early - late).coerceAtLeast(0)
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TimingCard("EARLY (0-5m)", early, Modifier.weight(1f))
        TimingCard("MID (5-20m)", mid, Modifier.weight(1f))
        TimingCard("LATE (20m+)", late, Modifier.weight(1f))
    }
}

@Composable
fun TimingCard(label: String, count: Int, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start
            )
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

fun countBursts(events: List<com.axios.focusguard.data.SessionEvent>): Int {
    var count = 0
    events.groupBy { it.packageName }.forEach { (_, appEvents) ->
        val sorted = appEvents.sortedBy { it.timestamp }
        var lastT: Long = 0
        sorted.forEach { e ->
            if (e.timestamp - lastT > 10000) count++
            lastT = e.timestamp
        }
    }
    return count
}

fun calculateFocusScore(events: List<com.axios.focusguard.data.SessionEvent>, bursts: Int): Int {
    if (events.isEmpty()) return 100
    var score = 100
    score -= (bursts * 8)
    score -= (events.size * 2)
    val early = events.count { it.sessionOffsetSeconds < 300 }
    if (early > 0) score -= 15
    return score.coerceIn(0, 100)
}

fun getScoreLabel(score: Int): String {
    return when {
        score >= 90 -> "Elite Focus"
        score >= 70 -> "Solid Effort"
        score >= 50 -> "Room for Growth"
        else -> "Total Meltdown"
    }
}
