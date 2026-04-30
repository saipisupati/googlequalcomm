package com.dragonbudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SnapdragonDark = darkColorScheme(
    primary = Color(0xFF00C2FF),       // electric blue
    onPrimary = Color(0xFF002A3D),
    secondary = Color(0xFF2DD4BF),     // teal
    onSecondary = Color(0xFF003D33),
    tertiary = Color(0xFFFF6A3D),      // ember orange
    onTertiary = Color(0xFF3D1500),
    background = Color(0xFF0A0E1A),    // near-black navy
    onBackground = Color(0xFFE6F4FF),
    surface = Color(0xFF12182B),       // raised card
    onSurface = Color(0xFFE6F4FF),
    surfaceVariant = Color(0xFF1B243F),
    onSurfaceVariant = Color(0xFFA8C2DD),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3D0000),
)

@Composable
fun DragonBudgetTheme(content: @Composable () -> Unit) {
    // Force dark scheme: hackathon look is "futuristic NPU console", not light mode.
    @Suppress("UNUSED_VARIABLE")
    val systemDark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = SnapdragonDark, content = content)
}
