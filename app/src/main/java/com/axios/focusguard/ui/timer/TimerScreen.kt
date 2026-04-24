package com.axios.focusguard.ui.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }

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
                val totalSeconds = when (uiState.sessionType) {
                    SessionType.FOCUS -> 25 * 60
                    SessionType.SHORT_BREAK -> 5 * 60
                    SessionType.LONG_BREAK -> 15 * 60
                }
                
                val elapsedProgress = (totalSeconds - uiState.timeLeftSeconds).toFloat() / totalSeconds
                
                val progress by animateFloatAsState(
                    targetValue = elapsedProgress,
                    animationSpec = tween(durationMillis = 1000),
                    label = "TimerProgress"
                )

                val progressColor by animateColorAsState(
                    targetValue = if (uiState.sessionType == SessionType.FOCUS) 
                        MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.primary, // Changed from hardcoded color to theme tertiary
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
                    
                    val sweepAngle = 360f * progress
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

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

            val buttonColor by animateColorAsState(
                targetValue = if (uiState.isRunning) 
                    MaterialTheme.colorScheme.surfaceVariant 
                    else if (uiState.sessionType == SessionType.FOCUS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                label = "ButtonColor"
            )

            Button(
                onClick = { viewModel.toggleTimer() },
                modifier = Modifier
                    .height(80.dp)
                    .width(200.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = if (uiState.isRunning) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                        else if (uiState.sessionType == SessionType.FOCUS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (uiState.isRunning) 0.dp else 8.dp,
                    pressedElevation = 2.dp
                ),
                enabled = uiState.hasPermissions || uiState.isRunning
            ) {
                Text(
                    text = if (uiState.isRunning) "PAUSE" else if (uiState.sessionType == SessionType.FOCUS) "START FOCUS" else "START BREAK",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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
            text = "SESSION ${uiState.completedFocusSessions + 1} OF 4",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 32.dp),
            letterSpacing = 2.sp
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
