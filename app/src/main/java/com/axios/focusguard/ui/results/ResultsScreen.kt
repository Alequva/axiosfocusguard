package com.axios.focusguard.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
            TopAppBar(title = { Text("Session Summary") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val rawEvents = uiState.events
                // Helper to count bursts (10s grouping)
                val totalBursts = mutableListOf<Long>().apply {
                    rawEvents.groupBy { it.packageName }.forEach { (_, events) ->
                        var lastT: Long = 0
                        events.sortedBy { it.timestamp }.forEach { e ->
                            if (e.timestamp - lastT > 10000) {
                                add(e.timestamp)
                            }
                            lastT = e.timestamp
                        }
                    }
                }.size
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (totalBursts == 0) "Perfect Focus!" else "Session Complete",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (totalBursts == 0) 
                                "You stayed perfectly focused!" 
                                else "${rawEvents.size} attempts in $totalBursts bursts",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (totalBursts == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        )
                        
                        if (totalBursts > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val early = uiState.events.count { it.sessionOffsetSeconds < 300 }
                            val late = uiState.events.count { it.sessionOffsetSeconds > 1200 }
                            val mid = totalBursts - early - late
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TimelineStat("Early", early)
                                TimelineStat("Mid", mid)
                                TimelineStat("Late", late)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.events.isNotEmpty()) {
                    Text(
                        text = "Distraction Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Group raw events into 10s bursts for display
                        val allBursts = mutableListOf<List<SessionEvent>>()
                        uiState.events.groupBy { it.packageName }.forEach { (_, appEvents) ->
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
                                        allBursts.add(currentBurst)
                                        currentBurst = mutableListOf(event)
                                    }
                                }
                            }
                            if (currentBurst.isNotEmpty()) allBursts.add(currentBurst)
                        }

                        items(allBursts.sortedByDescending { it.first().timestamp }) { burstEvents ->
                            AppViolationSummary(burstEvents.first().appName, burstEvents)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("✨", fontSize = 64.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onAnalyzeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("GET AI INSIGHTS")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        viewModel.resetToStart()
                        onNextSession()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("START NEXT SESSION")
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
