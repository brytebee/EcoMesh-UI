package com.brytebee.ecomesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Root composable for EcoMesh — shared across Android, iOS, Desktop, and Web.
 * This is the entry point for the Compose Multiplatform UI layer.
 */
@Composable
fun App() {
    EcoMeshTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EcoMeshHomeScreen()
        }
    }
}

@Composable
private fun EcoMeshHomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0F1E), Color(0xFF0D2137))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🌐 EcoMesh",
                color = Color(0xFF4FC3F7),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Offline · Eco-Aware · Secure",
                color = Color(0xFF90CAF9),
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Phase 1 complete — Foundation is live.",
                color = Color(0xFF80CBC4),
                fontSize = 14.sp,
            )
        }
    }
}
