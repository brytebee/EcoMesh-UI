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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import com.brytebee.ecomesh.ui.openFile
import androidx.compose.foundation.clickable


/**
 * Root composable for EcoMesh — shared across Android, iOS, Desktop, and Web.
 * This is the entry point for the Compose Multiplatform UI layer.
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    // getPlatformDisplayName() returns the SAME unique name registered with mDNS, ensuring
    // the handshake displayName and the name shown in Nearby Nodes always match.
    val meshCore = remember { 
        val nodeId = getPlatformNodeId()
        val displayName = getPlatformDisplayName()
        MeshCore(
            driverFactory = getDatabaseDriverFactory(),
            discoveryServices = getPlatformDiscoveryServices(),
            nodeId = nodeId,
            displayName = displayName
        )
    }
    
    val peers by meshCore.discoveryManager.peers.collectAsState()
    val thermalLevel by meshCore.discoveryManager.thermalService.thermalLevel.collectAsState()
    val wifiRequired by meshCore.discoveryManager.wifiRequired.collectAsState()
    val bluetoothRequired by meshCore.discoveryManager.bluetoothRequired.collectAsState()
    
    // Core Session & Messaging state
    val activeSessions by meshCore.sessionManager.activeSessions.collectAsState()
    val pendingRequests by meshCore.sessionManager.pendingRequests.collectAsState()
    val allMessages by meshCore.messagingManager.allMessages.collectAsState(initial = emptyList())
    val activeTransfers by meshCore.fileTransferService.activeTransfers.collectAsState()
    
    var connectingPeerId by remember { mutableStateOf<String?>(null) }
    var activeChatPeerId by remember { mutableStateOf<String?>(null) }
    // peerId → epoch-millis when the rejection cooldown expires (for UI countdown display)
    var rejectionCooldowns by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    LaunchedEffect(activeSessions) {
        // Sessions are keyed by connectionId, NOT peerId. Check values for matching nodeId or displayName.
        if (connectingPeerId != null &&
            activeSessions.values.any { it.nodeId == connectingPeerId || it.displayName == connectingPeerId }) {
            connectingPeerId = null
        }
    }

    LaunchedEffect(Unit) {
        launch { meshCore.start() }
        launch {
            meshCore.sessionManager.rejectedPeers.collect { event ->
                // Clear the connecting spinner for this peer
                if (connectingPeerId == event.targetPeerId) connectingPeerId = null
                // Record the cooldown expiry so the connect button can show a countdown
                rejectionCooldowns = rejectionCooldowns + (event.targetPeerId to event.canRetryAfterMs)
            }
        }
    }

    EcoMeshTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF070B14)
        ) {
            if (activeChatPeerId != null) {
                // Notify the messaging layer that this conversation is now open.
                // This triggers ReadReceipts for all unread messages.
                LaunchedEffect(activeChatPeerId) {
                    activeChatPeerId?.let { meshCore.messagingManager.markConversationRead(it) }
                }

                ChatScreen(
                    peerId = activeChatPeerId!!,
                    messages = allMessages.filter {
                        it.senderId == activeChatPeerId ||
                        (it.senderId == meshCore.sessionManager.localNodeId)
                    },
                    activeTransfers = activeTransfers,
                    onBack = {
                        meshCore.messagingManager.onChatClosed()
                        activeChatPeerId = null
                    },
                    onSendMessage = { text ->
                        scope.launch {
                            meshCore.messagingManager.sendMessage(activeChatPeerId!!, text)
                        }
                    },
                    onSendFile = { path, fileName ->
                        scope.launch {
                            val fileId = "file-${(100000..999999).random()}"
                            meshCore.messagingManager.sendMessage(activeChatPeerId!!, "📁 Shared file: $fileId|$fileName")
                            meshCore.fileTransferService.sendFile(
                                targetNodeId = activeChatPeerId!!,
                                fileId = fileId, // In a real app, use UUID
                                fileName = fileName,
                                filePath = path
                            )
                        }
                    },
                    onPauseTransfer = { fileId -> meshCore.fileTransferService.pauseTransfer(fileId) },
                    onResumeTransfer = { fileId -> meshCore.fileTransferService.resumeTransfer(fileId) },
                    onCancelTransfer = { fileId -> meshCore.fileTransferService.cancelTransfer(fileId) }
                )
            } else {
                EcoMeshHomeScreen(
                    meshCore = meshCore,
                    scope = scope,
                    peers = peers,
                    activeSessions = activeSessions,
                    pendingRequests = pendingRequests,
                    connectingPeerId = connectingPeerId,
                    thermalLevel = thermalLevel,
                    wifiRequired = wifiRequired,
                    bluetoothRequired = bluetoothRequired,
                    onConnect = { peer ->
                        // Extra guard to prevent re-connecting to already active peer
                        if (activeSessions.values.any { it.nodeId == peer.id || it.displayName == peer.id }) return@EcoMeshHomeScreen
                        
                        scope.launch {
                            connectingPeerId = peer.id
                            val success = meshCore.connectToPeer(peer)
                            if (!success) {
                                connectingPeerId = null
                            }
                        }
                    },
                    onAcceptRequest = { connId ->
                        scope.launch { meshCore.sessionManager.acceptRequest(connId) }
                    },
                    onDeclineRequest = { connId ->
                        scope.launch { meshCore.sessionManager.declineRequest(connId) }
                    },
                    onOpenChat = { peerNodeId ->
                        activeChatPeerId = peerNodeId
                    },
                    onSendFile = { peerNodeId ->
                        activeChatPeerId = peerNodeId 
                        // Note: To use rememberFilePicker within a non-composable callback securely, 
                        // we must hoist it. We'll add this inside ChatScreen later.
                    }
                )
            }
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
    activeSessions: Map<String, PeerSession>,
    pendingRequests: Map<String, com.brytebee.ecomesh.core.messaging.PendingRequest>,
    connectingPeerId: String?,
    thermalLevel: ThermalLevel,
    wifiRequired: Boolean,
    bluetoothRequired: Boolean,
    onConnect: (Peer) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onSendFile: (String) -> Unit
) {
    val transfers by meshCore.fileTransferService.activeTransfers.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0C192E), Color(0xFF030712)),
                    radius = 2500f,
                    center = androidx.compose.ui.geometry.Offset(0f, 0f)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mesh Map Visualisation
            if (thermalLevel < ThermalLevel.LEVEL_2_ECO) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    MeshMapCanvas(peers = peers, activeSessions = activeSessions)
                }
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "🌐",
                        fontSize = 40.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            Text(
                text = "EcoMesh",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Text(
                text = "SECURE NETWORK",
                color = Color(0xFF4FC3F7),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            
            Spacer(Modifier.height(32.dp))

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

            // WiFi Warning Alert (Glassy)
            if (wifiRequired) {
                Card(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFB300)),
                    border = BorderStroke(1.dp, Color(0x66FFB300))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📡", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "WiFi is OFF. Turn ON WiFi to discover peers.",
                            color = Color(0xFFFFE082),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Bluetooth Warning Alert (Glassy)
            if (bluetoothRequired) {
                Card(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x332196F3)),
                    border = BorderStroke(1.dp, Color(0x662196F3))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔵", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Bluetooth is OFF. Turn ON Bluetooth to advertise and scan.",
                            color = Color(0xFFBBDEFB),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            SessionStatusBar(sessionCount = activeSessions.size)

            Spacer(Modifier.height(16.dp))

            // Gossip Broadcast Button (Available if at least 1 session is active)
            if (activeSessions.isNotEmpty()) {
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

            // Active Sessions Section
            if (activeSessions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "ACTIVE SESSIONS",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    items(items = activeSessions.values.toList()) { session ->
                        SessionItem(
                            session = session,
                            onSendFile = { onSendFile(session.nodeId) },
                            onOpenChat = { onOpenChat(session.nodeId) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
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
                val selfName = getPlatformDisplayName()
                val externalPeers = peers.filter { 
                    it.id != meshCore.sessionManager.localNodeId && it.name != selfName 
                }
                items(externalPeers) { peer ->
                    val isAlreadyConnected = activeSessions.values.any { it.nodeId == peer.id || it.displayName == peer.id }
                    PeerItem(
                        peer = peer,
                        isConnecting = connectingPeerId == peer.id,
                        isAlreadyConnected = isAlreadyConnected,
                        onConnect = { onConnect(peer) }
                    )
                }
            }
        }
        
        // Floating Pending Requests Dialog Layer
        if (pendingRequests.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 400.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pendingRequests.values.toList()) { pending ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D3557)),
                            border = BorderStroke(1.dp, Color(0xFF4FC3F7))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(64.dp).background(Color(0x334FC3F7), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📡", fontSize = 28.sp)
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Incoming Connection Request",
                                    color = Color(0xFF4FC3F7),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    pending.displayName,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = { onDeclineRequest(pending.connId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5252)),
                                        border = BorderStroke(1.dp, Color(0xFFFF5252))
                                    ) {
                                        Text("Decline", color = Color(0xFFFFCDD2))
                                    }
                                    Button(
                                        onClick = { onAcceptRequest(pending.connId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionStatusBar(sessionCount: Int) {
    val (text, color) = if (sessionCount > 0) {
        "$sessionCount peer${if (sessionCount > 1) "s" else ""} connected" to Color(0xFF4CAF50)
    } else {
        "No active sessions" to Color(0xFF78909C)
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
fun SessionItem(
    session: PeerSession,
    onSendFile: () -> Unit,
    onOpenChat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x114CAF50)),
        border = BorderStroke(width = 1.dp, color = Color(0x224CAF50)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x334CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛡️", fontSize = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = session.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Secured • ${session.nodeId.take(8)}", color = Color(0xFF81C784), fontSize = 11.sp)
            }
            
            // Minimal Icon Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpenChat,
                    modifier = Modifier.size(40.dp).background(Color(0x1AFFFFFF), CircleShape)
                ) {
                    Text("💬", fontSize = 16.sp)
                }
                IconButton(
                    onClick = onSendFile,
                    modifier = Modifier.size(40.dp).background(Color(0x1AFFFFFF), CircleShape)
                ) {
                    Text("📎", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PeerItem(
    peer: Peer,
    isConnecting: Boolean,
    isAlreadyConnected: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0x08FFFFFF)),
        border = BorderStroke(width = 1.dp, color = Color(0x1AFFFFFF)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x1A4FC3F7)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (peer.type == PeerType.MOBILE) "📱" else "💻", fontSize = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                // Minimalist dot instead of text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF4FC3F7), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(text = "Nearby", color = Color(0x88FFFFFF), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            
            // Connection Action Widget
            if (isAlreadyConnected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x224CAF50))
                        .border(1.dp, Color(0x444CAF50), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp)
                ) {
                    Text("✓", color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else if (isConnecting) {
                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF4FC3F7), strokeWidth = 2.dp)
                }
            } else {
                IconButton(
                    onClick = onConnect,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x1A4FC3F7), CircleShape)
                ) {
                    Text("🔗", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TransferItem(
    transfer: TransferProgress,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val isPaused = transfer.status == "PAUSED" || transfer.status == "FAILED"
    val isEncrypting = transfer.status == "ENCRYPTING" || transfer.status == "PREPARING"
    val isActive = transfer.status == "SENDING" || transfer.status == "IN_PROGRESS" || isEncrypting
    
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaused) Color(0x1A6A1B9A) else Color(0x0AFFFFFF)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                transfer.status == "FAILED" -> Color(0x44FF5252)
                isPaused -> Color(0x44FFB300)
                isEncrypting -> Color(0x444FC3F7)
                else -> Color(0x1AFFFFFF)
            }
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Icon with State-driven Pulsing
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer(alpha = if (isEncrypting) pulseAlpha else 1f)
                        .background(
                            color = when {
                                transfer.status == "COMPLETED" -> Color(0x334CAF50)
                                transfer.status == "FAILED" -> Color(0x33FF5252)
                                else -> Color(0x1A4FC3F7)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEncrypting) "🔐" else "📄",
                        fontSize = 14.sp
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                // Filename
                Text(
                    text = transfer.fileName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Pure Icon Controls (Action Suite)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isActive) {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier.size(32.dp).background(Color(0x1AFFFFFF), CircleShape)
                        ) {
                            Text("⏸️", fontSize = 12.sp)
                        }
                    } else if (isPaused) {
                        IconButton(
                            onClick = onResume,
                            modifier = Modifier.size(32.dp).background(Color(0x1AFFFFFF), CircleShape)
                        ) {
                            Text("▶️", fontSize = 12.sp)
                        }
                    }
                    
                    if (transfer.status != "COMPLETED") {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(32.dp).background(Color(0x1AFF5252), CircleShape)
                        ) {
                            Text("✖️", fontSize = 12.sp)
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(32.dp).background(Color(0x224CAF50), CircleShape)
                        ) {
                            Text("✓", color = Color(0xFF64FFDA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Ultra-thin glowing progress tracker (integrated into the pill's bottom)
            if (transfer.status != "COMPLETED" && transfer.status != "FAILED") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0x11FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(transfer.progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF4FC3F7), Color(0xFF64FFDA))
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    peerId: String,
    messages: List<com.brytebee.ecomesh.core.messaging.ChatMessageModel>,
    activeTransfers: Map<String, com.brytebee.ecomesh.core.messaging.TransferProgress>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendFile: (String, String) -> Unit,
    onPauseTransfer: (String) -> Unit = {},
    onResumeTransfer: (String) -> Unit = {},
    onCancelTransfer: (String) -> Unit = {}
) {
    var textState by remember { mutableStateOf("") }
    
    val filePickerLauncher = rememberFilePicker { path, name ->
        if (path != null && name != null) {
            onSendFile(path, name)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0C192E), Color(0xFF030712)),
                    radius = 2500f,
                    center = androidx.compose.ui.geometry.Offset(0f, 0f)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            // Glassmorphism Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp).background(Color(0x1AFFFFFF), CircleShape)
                    ) {
                        Text("🔙", fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SECURE CHAT",
                            color = Color(0xFF4FC3F7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF64FFDA), CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = peerId.take(12),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Messages
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages.reversed(), key = { it.id }) { msg ->
                    val isMe = msg.senderId != peerId
                    
                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { isVisible = true }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInHorizontally(
                            initialOffsetX = { if (isMe) it else -it },
                            animationSpec = tween(400, easing = LinearOutSlowInEasing)
                        ) + fadeIn(tween(400)),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            GlassyMessageBubble(
                                isMe = isMe,
                                content = msg.content,
                                timestamp = msg.timestamp,
                                status = msg.status,
                                activeTransfers = activeTransfers,
                                onPauseTransfer = onPauseTransfer,
                                onResumeTransfer = onResumeTransfer,
                                onCancelTransfer = onCancelTransfer
                            )
                        }
                    }
                }
            }

            // Floating Input Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // File Attachment Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                        .clickable { filePickerLauncher() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📎", fontSize = 20.sp)
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp, max = 120.dp),
                        placeholder = { Text("Type a message...", color = Color(0x66FFFFFF), fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            cursorColor = Color(0xFF4FC3F7),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                
                val isInputActive = textState.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isInputActive) {
                                Brush.linearGradient(listOf(Color(0xFF0288D1), Color(0xFF01579B)))
                            } else {
                                Brush.linearGradient(listOf(Color(0x1AFFFFFF), Color(0x0AFFFFFF)))
                            }
                        )
                        .border(1.dp, if (isInputActive) Color.Transparent else Color(0x22FFFFFF), CircleShape)
                        .clickable(enabled = isInputActive) {
                            onSendMessage(textState)
                            textState = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🚀",
                        fontSize = 20.sp,
                        color = if (isInputActive) Color.White else Color(0x66FFFFFF)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassyMessageBubble(
    isMe: Boolean,
    content: String,
    timestamp: Long,
    status: String,
    activeTransfers: Map<String, TransferProgress>,
    onPauseTransfer: (String) -> Unit,
    onResumeTransfer: (String) -> Unit,
    onCancelTransfer: (String) -> Unit
) {
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp)
    }

    Box(
        modifier = Modifier
            .widthIn(min = 60.dp, max = 300.dp)
            .background(
                brush = if (isMe) {
                    Brush.linearGradient(listOf(Color(0xFF0288D1), Color(0xFF01579B)))
                } else {
                    Brush.linearGradient(listOf(Color(0x1AFFFFFF), Color(0x0AFFFFFF)))
                },
                shape = bubbleShape
            )
            .border(
                width = 1.dp,
                color = if (isMe) Color(0x33FFFFFF) else Color(0x1AFFFFFF),
                shape = bubbleShape
            )
            .shadow(
                elevation = if (isMe) 12.dp else 4.dp,
                shape = bubbleShape,
                ambientColor = if (isMe) Color(0xFF0288D1) else Color.Black,
                spotColor = if (isMe) Color(0xFF0288D1) else Color.Black
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = content,
                color = if (isMe) Color.White else Color(0xFFE1F5FE),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )
            
            val filePrefix = "📁 Shared file: "
            if (content.startsWith(filePrefix)) {
                val remainder = content.removePrefix(filePrefix)
                val parts = remainder.split("|", limit = 2)
                val fileId = if (parts.size == 2) parts[0] else ""
                val fileName = if (parts.size == 2) parts[1] else remainder
                
                val transfer = if (fileId.isNotEmpty()) {
                    activeTransfers[fileId]
                } else {
                    activeTransfers.values.find { it.fileName == fileName && it.status != "IN_PROGRESS" && it.status != "SENDING" }
                }

                if (transfer != null) {
                    Spacer(Modifier.height(10.dp))
                    TransferItem(
                        transfer = transfer,
                        onPause = { onPauseTransfer(transfer.fileId) },
                        onResume = { onResumeTransfer(transfer.fileId) },
                        onCancel = { onCancelTransfer(transfer.fileId) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTimestamp(timestamp),
                    color = if (isMe) Color(0xAAFFFFFF) else Color(0x88E1F5FE),
                    fontSize = 10.sp
                )
                if (isMe) {
                    Spacer(Modifier.width(4.dp))
                    val (tickText, tickColor) = when (status) {
                        "SENDING"         -> "⌚" to Color(0x66FFFFFF)
                        "SENT"            -> "✓" to Color(0xAAFFFFFF)
                        "DELIVERED"       -> "✓✓" to Color(0xAAFFFFFF)
                        "READ"            -> "✓✓" to Color(0xFF64FFDA) // Cyan = read
                        "FAILED"          -> "✗" to Color(0xFFFF5252)
                        else              -> "✓" to Color(0xAAFFFFFF)
                    }
                    Text(
                        text = tickText,
                        color = tickColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun formatTimestamp(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

@Composable
fun MeshMapCanvas(peers: List<com.brytebee.ecomesh.core.discovery.Peer>, activeSessions: Map<String, com.brytebee.ecomesh.core.messaging.PeerSession>) {
    val infiniteTransition = rememberInfiniteTransition()
    val radarSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2.2f
        
        // Dark glassy rings
        for (i in 1..3) {
            drawCircle(
                color = Color(0x1A4FC3F7), // Ultra-faint cyan rings
                radius = maxRadius * (i / 3f),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
        
        // Radar Sweep Conic Gradient
        drawArc(
            brush = Brush.sweepGradient(
                0.0f to Color.Transparent,
                0.8f to Color.Transparent,
                1.0f to Color(0x334FC3F7) // Fades into glowing edge
            ),
            startAngle = radarSweep - 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - maxRadius, center.y - maxRadius),
            size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
        )
        
        // Draw Core Device Center
        drawCircle(color = Color(0xFF64FFDA), radius = 16f, center = center)
        drawCircle(color = Color(0x4464FFDA), radius = 28f, center = center)
        
        // Interconnected Peers
        val angleStep = if (peers.isNotEmpty()) (2 * kotlin.math.PI / peers.size).toFloat() else 0f
        peers.forEachIndexed { index, peer ->
            val isConnected = activeSessions.values.any { it.nodeId == peer.id || it.displayName == peer.id }
            
            // Connected peers orbit closer, discovered orbit further
            val orbitRadius = if (isConnected) maxRadius * 0.6f else maxRadius * 0.9f
            
            // Dynamic angle drifting using simple index offsets
            val angle = index * angleStep + (if (!isConnected) radarSweep * 0.005f else 0f)
            
            val x = center.x + kotlin.math.cos(angle) * orbitRadius
            val y = center.y + kotlin.math.sin(angle) * orbitRadius
            val peerOffset = androidx.compose.ui.geometry.Offset(x, y)
            
            if (isConnected) {
                // Neon glowing link line
                drawLine(
                    color = Color(0x8864FFDA),
                    start = center,
                    end = peerOffset,
                    strokeWidth = 3f
                )
                // Peer Core
                drawCircle(color = Color(0xFF4CAF50), radius = 12f, center = peerOffset)
                // Outer Glow
                drawCircle(color = Color(0x334CAF50), radius = 24f, center = peerOffset)
            } else {
                // Faint offline dot
                drawCircle(color = Color(0x66FFFFFF), radius = 8f, center = peerOffset)
            }
        }
    }
}
