package com.axios.focusguard.ui.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.axios.focusguard.domain.model.TimerPreset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onSessionFinished: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val selectedPresetId by viewModel.selectedPresetId.collectAsState()
    
    var showPresetsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Removed top row with icons

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.hasPermissions && !uiState.isRunning) {
                PermissionWarning(onClick = onSettingsClick)
                Spacer(modifier = Modifier.height(32.dp))
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                val totalSeconds = uiState.initialSessionSeconds
                
                // Clockwise Filling: 0.0 (start) up to 1.0 (end)
                // We show how much time has ELAPSED to make it move clockwise left-to-right
                val elapsedProgress = (totalSeconds - uiState.timeLeftSeconds).toFloat() / totalSeconds
                
                val progress by animateFloatAsState(
                    targetValue = elapsedProgress,
                    animationSpec = tween(durationMillis = 1000),
                    label = "TimerProgress"
                )

                val progressColor by animateColorAsState(
                    targetValue = if (uiState.sessionType == SessionType.FOCUS) 
                        MaterialTheme.colorScheme.primary 
                        else Color(0xFFB1FD54),
                    label = "ProgressColor"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Background Circle
                    drawCircle(
                        color = progressColor.copy(alpha = 0.05f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // True Clockwise Sweep: Start at -90 (Top) and sweep right
                    val sweepAngle = 360f * progress
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Comet Head: Locked to the LEADING edge of the clockwise sweep
                    val angleInRad = (sweepAngle - 90f) * (Math.PI / 180f).toFloat()
                    val endCircleX = center.x + radius * cos(angleInRad)
                    val endCircleY = center.y + radius * sin(angleInRad)
                    
                    drawCircle(
                        color = progressColor,
                        radius = (strokeWidth * 2f),
                        center = Offset(endCircleX, endCircleY)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.sessionType.name.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        color = progressColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTime(uiState.timeLeftSeconds),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Presets Button
                ActionButton(
                    icon = Icons.Default.List,
                    onClick = { showPresetsSheet = true },
                    contentDescription = "Presets"
                )

                Spacer(modifier = Modifier.width(32.dp))

                // Play/Pause Button
                val playIcon = if (uiState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow
                ActionButton(
                    icon = playIcon,
                    onClick = { viewModel.toggleTimer() },
                    contentDescription = if (uiState.isRunning) "Pause" else "Play",
                    isMain = true,
                    enabled = uiState.hasPermissions || uiState.isRunning
                )

                Spacer(modifier = Modifier.width(32.dp))

                // Settings Button
                ActionButton(
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick,
                    contentDescription = "Settings"
                )
            }
            
            if (uiState.isRunning) {
                TextButton(
                    onClick = { viewModel.finishSessionEarly(); onSessionFinished() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("DEBUG: FINISH SESSION", color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Text(
            text = "SESSION ${uiState.completedFocusSessions + 1} OF ${uiState.totalRounds}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 32.dp),
            letterSpacing = 2.sp
        )
    }

    if (showPresetsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPresetsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
        ) {
            PresetBottomSheetContent(
                presets = presets,
                selectedPresetId = selectedPresetId,
                onSelect = { 
                    viewModel.selectPreset(it)
                    showPresetsSheet = false
                },
                onCreateCustom = { name, focus, breakTime, rounds ->
                    viewModel.createCustomPreset(name, focus, breakTime, rounds)
                }
            )
        }
    }
}

@Composable
fun PresetBottomSheetContent(
    presets: List<TimerPreset>,
    selectedPresetId: String,
    onSelect: (String) -> Unit,
    onCreateCustom: (String, Int, Int, Int) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var infoPreset by remember { mutableStateOf<TimerPreset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Timer Presets",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Preset", tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            presets.forEach { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = preset.id == selectedPresetId,
                    onClick = { onSelect(preset.id) },
                    onInfoClick = { infoPreset = preset }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showCreateDialog) {
        CreatePresetDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, focus, breakTime, rounds ->
                onCreateCustom(name, focus, breakTime, rounds)
                showCreateDialog = false
            }
        )
    }

    infoPreset?.let { preset ->
        PresetInfoDialog(
            preset = preset,
            onDismiss = { infoPreset = null }
        )
    }
}

@Composable
fun PresetCard(
    preset: TimerPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    // Near background color, but greenish
    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color(0xFF252B21) // Dark greenish
    }
    
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${preset.focusTimeMin}m focus • ${preset.breakTimeMin}m break • ${preset.rounds} rounds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (preset.isInfo) {
                IconButton(onClick = { 
                    onInfoClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var focusTime by remember { mutableStateOf("25") }
    var breakTime by remember { mutableStateOf("5") }
    var rounds by remember { mutableStateOf("4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Preset") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = focusTime,
                        onValueChange = { focusTime = it },
                        label = { Text("Focus (min)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = breakTime,
                        onValueChange = { breakTime = it },
                        label = { Text("Break (min)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = rounds,
                    onValueChange = { rounds = it },
                    label = { Text("Rounds") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val f = focusTime.toIntOrNull() ?: 25
                val b = breakTime.toIntOrNull() ?: 5
                val r = rounds.toIntOrNull() ?: 4
                if (name.isNotBlank()) {
                    onConfirm(name, f, b, r)
                }
            }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PresetInfoDialog(
    preset: TimerPreset,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(preset.name) },
        text = {
            Text(preset.infoDescription ?: "No description available.")
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun ActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    isMain: Boolean = false,
    enabled: Boolean = true
) {
    val size = if (isMain) 80.dp else 56.dp
    val iconSize = if (isMain) 40.dp else 28.dp
    val bgColor = Color(0xFF252B21) // Near background greenish
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun PermissionWarning(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Permissions required to block apps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("FIX IN SETTINGS", color = MaterialTheme.colorScheme.onError)
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
