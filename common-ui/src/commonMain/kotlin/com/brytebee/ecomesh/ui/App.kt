package com.brytebee.ecomesh.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brytebee.ecomesh.core.discovery.*
import com.brytebee.ecomesh.core.transport.*
import com.brytebee.ecomesh.core.thermal.ThermalLevel
import com.brytebee.ecomesh.core.MeshCore
import com.brytebee.ecomesh.core.messaging.*
import com.brytebee.ecomesh.core.db.getDatabaseDriverFactory
import kotlinx.coroutines.launch
import com.brytebee.ecomesh.core.messaging.TransferProgress
import com.brytebee.ecomesh.core.messaging.MeshHandshakeState

/**
 * Root composable for EcoMesh — shared across Android, iOS, Desktop, and Web.
 * This is the entry point for the Compose Multiplatform UI layer.
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val meshCore = remember { 
        MeshCore(
            driverFactory = getDatabaseDriverFactory(),
            // Only use real platform discovery — MockDiscoveryService creates fake peers
            // with no host/port, causing silent failures and infinite loading spinners.
            discoveryServices = getPlatformDiscoveryServices(),
            nodeId = "mesh-node-${(1000..9999).random()}",
            displayName = "${getPlatformName()} Node" 
        )
    }
    
    val peers by meshCore.discoveryManager.peers.collectAsState()
    val thermalLevel by meshCore.discoveryManager.thermalService.thermalLevel.collectAsState()
    
    val currentHandshakeState by meshCore.handshakeState.collectAsState()
    var connectingPeerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentHandshakeState) {
        // Clear the loading spinner on any terminal state
        if (currentHandshakeState !is MeshHandshakeState.Negotiating) {
            connectingPeerId = null
        }
    }

    LaunchedEffect(Unit) {
        meshCore.start()
    }

    EcoMeshTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF070B14)
        ) {
            EcoMeshHomeScreen(
                meshCore = meshCore,
                scope = scope,
                peers = peers,
                connectingPeerId = connectingPeerId,
                handshakeState = currentHandshakeState,
                thermalLevel = thermalLevel,
                onConnect = { peer ->
                    scope.launch {
                        connectingPeerId = peer.id
                        val success = meshCore.connectToPeer(peer)
                        // If connection failed before handshake even started, clear spinner
                        if (!success) {
                            connectingPeerId = null
                        }
                    }
                },
                onSendFile = { peer ->
                    scope.launch {
                        meshCore.fileTransferService.sendFile(
                            fileId = "file-${(100..999).random()}",
                            fileName = "EcoMesh_Report.pdf",
                            filePath = "mock_report.pdf"
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun MeshPulseAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                .background(Color(0xFF4FC3F7), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(0xFF4FC3F7), CircleShape)
        )
    }
}

@Composable
fun EcoMeshHomeScreen(
    meshCore: MeshCore,
    scope: kotlinx.coroutines.CoroutineScope,
    peers: List<Peer>,
    connectingPeerId: String?,
    handshakeState: MeshHandshakeState,
    thermalLevel: ThermalLevel,
    onConnect: (Peer) -> Unit,
    onSendFile: (Peer) -> Unit
) {
    val transfers by meshCore.fileTransferService.activeTransfers.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF070B14)),
                    radius = 2000f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Pulse
            Box(contentAlignment = Alignment.Center) {
                if (thermalLevel < ThermalLevel.LEVEL_2_ECO) {
                    MeshPulseAnimation(modifier = Modifier.size(100.dp))
                }
                Text(
                    text = "🌐",
                    fontSize = 40.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Text(
                text = "EcoMesh",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Autonomous · Distributed · Resilient",
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
            
            Spacer(Modifier.height(40.dp))

            // Thermal Alert (Glassy)
            if (thermalLevel >= ThermalLevel.LEVEL_2_ECO) {
                Card(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FF5252)),
                    border = BorderStroke(1.dp, Color(0x66FF5252))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Thermal Safety Active. Features limited.",
                            color = Color(0xFFFFCDD2),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Connection Status Bar
            HandshakeStatusBar(handshakeState)

            Spacer(Modifier.height(16.dp))

            // Gossip Broadcast Button
            if (handshakeState is MeshHandshakeState.Authenticated) {
                Button(
                    onClick = {
                        scope.launch {
                            meshCore.gossipManager.publishGossip("ALERT", "🚨 Emergency Mesh Alert!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22F44336)),
                    border = BorderStroke(1.dp, Color(0x44F44336)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📢 SHARE ALERT (GOSSIP)", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // Active Transfers Section
            if (transfers.isNotEmpty()) {
                Text(
                    text = "ACTIVE TRANSFERS",
                    color = Color(0xFF4FC3F7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = transfers.values.toList()) { transfer ->
                        TransferItem(transfer)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "NEARBY NODES",
                        color = Color(0xFF90CAF9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                items(peers) { peer ->
                    PeerItem(
                        peer = peer,
                        isConnecting = connectingPeerId == peer.id,
                        handshakeState = handshakeState,
                        onConnect = { onConnect(peer) },
                        onSendFile = { onSendFile(peer) }
                    )
                }
            }
        }
    }
}

@Composable
fun HandshakeStatusBar(state: MeshHandshakeState) {
    val (text, color) = when (state) {
        is MeshHandshakeState.Idle -> "Standby" to Color(0xFF78909C)
        is MeshHandshakeState.Negotiating -> "Negotiating Encryption..." to Color(0xFFFFB300)
        is MeshHandshakeState.Authenticated -> "Securely Connected" to Color(0xFF4CAF50)
        is MeshHandshakeState.Failed -> "Handshake Failed" to Color(0xFFF44336)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PeerItem(
    peer: Peer,
    isConnecting: Boolean,
    handshakeState: MeshHandshakeState,
    onConnect: () -> Unit,
    onSendFile: () -> Unit
) {
    // isPeerConnected: once Authenticated, highlight the peer we were connecting to.
    // We can't compare peerNodeId to peer.id directly because mDNS service name != handshake nodeId.
    // Instead, we just show all peers as connected when Authenticated (single-peer bridge model).
    val isPeerConnected = handshakeState is MeshHandshakeState.Authenticated
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPeerConnected) Color(0x224CAF50) else Color(0x11FFFFFF)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPeerConnected) Color(0x444CAF50) else Color(0x22FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Peer Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isPeerConnected) Color(0xFF4CAF50) else Color(0xFF1D3557)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (peer.type == PeerType.MOBILE) "📱" else "💻",
                    fontSize = 20.sp
                )
            }
            
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
                Text(
                    text = peer.id.take(12),
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (isConnecting && !isPeerConnected) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFFFFB300),
                    strokeWidth = 2.dp
                )
            } else if (isPeerConnected) {
                Text(
                    text = "CONNECTED",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                )
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4FC3F7),
                        contentColor = Color(0xFF070B14)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("CONNECT", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }

            if (isPeerConnected) {
                IconButton(onClick = onSendFile) {
                    Text("📁", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun TransferItem(transfer: TransferProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📄", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(transfer.fileName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(transfer.status, color = if (transfer.status == "COMPLETED") Color(0xFF4CAF50) else Color(0xFFB0BEC5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = transfer.progress,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = Color(0xFF4FC3F7),
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}
