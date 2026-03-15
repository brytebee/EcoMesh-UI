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
import com.brytebee.ecomesh.core.transport.*
import com.brytebee.ecomesh.core.thermal.ThermalLevel
import kotlinx.coroutines.launch

/**
 * Root composable for EcoMesh — shared across Android, iOS, Desktop, and Web.
 * This is the entry point for the Compose Multiplatform UI layer.
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val discoveryManager = remember { 
        DiscoveryManager(getPlatformDiscoveryServices() + MockDiscoveryService()) 
    }
    val transportService = remember { getPlatformTransportService() }
    val peers by discoveryManager.peers.collectAsState()
    val thermalLevel by discoveryManager.thermalService.thermalLevel.collectAsState()
    
    var connectingPeerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        discoveryManager.start()
    }

    EcoMeshTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EcoMeshHomeScreen(
                peers = peers,
                connectingPeerId = connectingPeerId,
                thermalLevel = thermalLevel,
                onConnect = { peer ->
                    scope.launch {
                        connectingPeerId = peer.id
                        val success = transportService.connect(peer.id)
                        if (success) {
                            // In real app, we'd wait for handshake here
                        }
                        connectingPeerId = null
                    }
                }
            )
        }
    }
}

@Composable
private fun EcoMeshHomeScreen(
    peers: List<Peer>,
    connectingPeerId: String?,
    thermalLevel: ThermalLevel,
    onConnect: (Peer) -> Unit
) {
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
            
            if (thermalLevel >= ThermalLevel.LEVEL_2_ECO) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌿 Cooling Active: Mesh capabilities throttled to protect hardware.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            if (thermalLevel < ThermalLevel.LEVEL_2_ECO) {
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
            } else {
                Text(
                    text = "Discovery Paused (Thermal Protection)",
                    color = Color.Gray,
                    fontSize = 14.sp,
                )
            }

            Spacer(Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(peers) { peer ->
                    PeerItem(
                        peer = peer,
                        isConnecting = connectingPeerId == peer.id,
                        onConnect = { onConnect(peer) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeerItem(
    peer: Peer,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
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
            
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF4FC3F7),
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D3557)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Connect", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
