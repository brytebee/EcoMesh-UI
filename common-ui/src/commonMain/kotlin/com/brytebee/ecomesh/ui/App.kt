package com.brytebee.ecomesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brytebee.ecomesh.core.discovery.*

/**
 * Root composable for EcoMesh — shared across Android, iOS, Desktop, and Web.
 * This is the entry point for the Compose Multiplatform UI layer.
 */
@Composable
fun App() {
    val discoveryManager = remember { 
        DiscoveryManager(getPlatformDiscoveryServices() + MockDiscoveryService()) 
    }
    val peers by discoveryManager.peers.collectAsState()

    LaunchedEffect(Unit) {
        discoveryManager.start()
    }

    EcoMeshTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EcoMeshHomeScreen(peers)
        }
    }
}

@Composable
private fun EcoMeshHomeScreen(peers: List<Peer>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0F1E), Color(0xFF0D2137))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🌐 EcoMesh",
                color = Color(0xFF4FC3F7),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Offline · Eco-Aware · Secure",
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
            )
            
            Spacer(Modifier.height(32.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF80CBC4)
                )
                Text(
                    text = "Scanning for nearby peers...",
                    color = Color(0xFF80CBC4),
                    fontSize = 14.sp,
                )
            }

            Spacer(Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(peers) { peer ->
                    PeerItem(peer)
                }
            }
        }
    }
}

@Composable
private fun PeerItem(peer: Peer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF112240)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${peer.type} · ${peer.id}",
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp
                )
            }
            
            Text(
                text = "${peer.rssi ?: "--"} dBm",
                color = Color(0xFF80CBC4),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
