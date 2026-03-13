package com.brytebee.ecomesh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── EcoMesh Design Tokens ──────────────────────────────────────────────────
private val DeepNavy = Color(0xFF0A0F1E)
private val OceanBlue = Color(0xFF0D2137)
private val ElectricCyan = Color(0xFF4FC3F7)
private val SoftBlue = Color(0xFF90CAF9)
private val EcoGreen = Color(0xFF26A69A)
private val WarnAmber = Color(0xFFFFB300)
private val AlertRed = Color(0xFFEF5350)
private val SurfaceCard = Color(0xFF112240)

private val EcoMeshColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepNavy,
    primaryContainer = OceanBlue,
    secondary = EcoGreen,
    onSecondary = DeepNavy,
    background = DeepNavy,
    surface = SurfaceCard,
    onBackground = SoftBlue,
    onSurface = SoftBlue,
    error = AlertRed,
    tertiary = WarnAmber,
)

@Composable
fun EcoMeshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EcoMeshColorScheme,
        content = content,
    )
}
