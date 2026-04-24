package com.axios.focusguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColors = darkColorScheme(
    primary         = Color(0xFFFFFFFF),
    onPrimary       = Color(0xFF000000),
    background      = Color(0xFF0D0D0D),
    onBackground    = Color(0xFFFFFFFF),
    surface         = Color(0xFF1A1A1A),
    onSurface       = Color(0xFFFFFFFF),
    surfaceVariant  = Color(0xFF2A2A2A),
    outline         = Color(0xFF444444)
)

private val BlueColors = darkColorScheme(
    primary         = Color(0xFF2496C7),
    onPrimary       = Color(0xFFE8EBF5),
    background      = Color(0xFF212121),
    onBackground    = Color(0xFFE8EBF5),
    surface         = Color(0xFF232323),
    onSurface       = Color(0xFFE8EBF5),
    surfaceVariant  = Color(0xFF2B2B2B),
    outline         = Color(0xFF353535)
)

private val VioletColors = darkColorScheme(
    primary         = Color(0xFF9254FD),
    onPrimary       = Color(0xFFE8EBF5),
    background      = Color(0xFF212121),
    onBackground    = Color(0xFFE8EBF5),
    surface         = Color(0xFF232323),
    onSurface       = Color(0xFFE8EBF5),
    surfaceVariant  = Color(0xFF2B2B2B),
    outline         = Color(0xFF353535)
)

private val GreenColors = darkColorScheme(
    primary         = Color(0xFFB1FD54),
    onPrimary       = Color(0xFF191A19),
    background      = Color(0xFF212121),
    onBackground    = Color(0xFFF1F5E8),
    surface         = Color(0xFF232323),
    onSurface       = Color(0xFFF1F5E8),
    surfaceVariant  = Color(0xFF2B2B2B),
    outline         = Color(0xFF4E4E4E)
)


@Composable
fun FocusGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GreenColors,
        content     = content
    )
}
