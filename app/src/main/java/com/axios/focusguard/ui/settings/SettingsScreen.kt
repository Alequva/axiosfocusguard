package com.axios.focusguard.ui.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.axios.focusguard.data.AppInfo
import com.axios.focusguard.data.AppCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updatePermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                PermissionSection(
                    isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                    isOverlayEnabled = uiState.isOverlayEnabled,
                    onAccessibilityClick = { viewModel.openAccessibilitySettings() },
                    onOverlayClick = { viewModel.openOverlaySettings() }
                )
            }

            item {
                Text(
                    text = "Block Distractor Apps",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(uiState.apps) { app ->
                    AppItem(
                        app = app,
                        onToggle = { viewModel.toggleAppBlock(app) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionSection(
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    onAccessibilityClick: () -> Unit,
    onOverlayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text("Required Permissions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        PermissionItem(
            title = "Accessibility Service",
            isGranted = isAccessibilityEnabled,
            onClick = onAccessibilityClick
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionItem(
            title = "Display Over Other Apps",
            isGranted = isOverlayEnabled,
            onClick = onOverlayClick
        )
    }
}

@Composable
fun PermissionItem(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (!isGranted) {
            Button(
                onClick = onClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("GRANT", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconBitmap = remember(app.packageName) { app.icon?.toBitmap()?.asImageBitmap() }
        
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = app.name, style = MaterialTheme.typography.bodyLarge)
                if (app.category != AppCategory.OTHER) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = app.category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Checkbox(checked = app.isBlocked, onCheckedChange = { onToggle() })
    }
}

private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return bitmap
    val bitmap = Bitmap.createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
