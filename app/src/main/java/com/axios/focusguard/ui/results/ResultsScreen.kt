package com.axios.focusguard.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.axios.focusguard.data.SessionEvent
import com.axios.focusguard.ui.components.MascotImage
import com.axios.focusguard.ui.components.MascotPose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onAnalyzeClick: () -> Unit,
    onNextSession: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Session Summary", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val rawEvents = uiState.events
                val bursts = mutableListOf<Long>().apply {
                    rawEvents.groupBy { it.packageName }.forEach { (_, events) ->
                        var lastT: Long = 0
                        events.sortedBy { it.timestamp }.forEach { e ->
                            if (e.timestamp - lastT > 10000) {
                                add(e.timestamp)
                            }
                            lastT = e.timestamp
                        }
                    }
                }
                val totalBursts = bursts.size
                
                // Content Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    MascotImage(
                        pose = if (totalBursts == 0) MascotPose.YAY else MascotPose.CLOCK,
                        size = 180.dp
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "DISTRACTION BURSTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 2.sp
                    )
                    
                    Text(
                        text = totalBursts.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = if (totalBursts == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Visual Timeline Bar
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                        ) {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                                val width = maxWidth
                                for (event in rawEvents) {
                                    val ratio = if (uiState.sessionDurationSeconds > 0) {
                                        (event.sessionOffsetSeconds.toFloat() / uiState.sessionDurationSeconds.toFloat()).coerceIn(0f, 1f)
                                    } else 0f
                                    
                                    Box(
                                        modifier = Modifier
                                            .offset(x = width * ratio - 4.dp)
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
                                            .align(Alignment.CenterStart)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Start", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("Finish", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(64.dp))
                    
                    // Offender List with Hierarchy
                    if (rawEvents.isNotEmpty()) {
                        val appGroups = rawEvents.groupBy { it.packageName }.values.sortedByDescending { it.size }
                        
                        Text(
                            text = "OFFENDING APPS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().animateContentSize()
                        ) {
                            for (index in appGroups.indices) {
                                val group = appGroups[index]
                                val appName = group.first().appName
                                val category = group.first().category
                                val attempts = group.size
                                val isTop = index == 0
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isTop) 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(if (isTop) 16.dp else 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(if (isTop) 20.dp else 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = appName, 
                                                fontWeight = if (isTop) FontWeight.ExtraBold else FontWeight.Bold, 
                                                style = if (isTop) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = category, 
                                                color = if (isTop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, 
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        Surface(
                                            color = if (isTop) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$attempts",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isTop) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        ) {
                            Text(
                                text = "✨",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Perfect focus streak.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No apps were blocked this session.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Area
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.resetToStart()
                            onNextSession()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("START NEXT SESSION", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onAnalyzeClick,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                    ) {
                        Text("GET AI INSIGHTS", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStat(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppViolationSummary(appName: String, events: List<SessionEvent>) {
    val category = events.firstOrNull()?.category ?: "OTHER"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = appName, fontWeight = FontWeight.Bold)
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (events.size > 1) "${events.size} attempts" else "1 attempt",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val firstBurst = events.minOf { it.sessionOffsetSeconds } / 60
            Text(
                text = "First distracted at ${firstBurst}m into session",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
